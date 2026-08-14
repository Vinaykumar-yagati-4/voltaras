#!/usr/bin/env bash
# ============================================================================
# VOLTARAS - Docker demo data seeder (bash / Git Bash / WSL)
#
# Creates the local demo dataset used for Swagger/OpenAPI verification of the
# VOLTARAS microservice stack running in Docker Compose.
#
#   * 30 demo users  (1 ADMIN = sunny, 29 CONSUMER)
#   * user profiles   (user-service)
#   * 1 demo organization + memberships (organization-service)
#   * 30 meters assigned to consumers (meter-management-service)
#   * 30 verified meter readings (meter-reading-service)
#   * 30 bills (bill-service)
#   * wallet top-ups + bill payments (payment-service)
#   * complaints (complaint-service)
#
# Design rules honoured by this script:
#   1. No real personal data - only clean demo names / Hyderabad demo addresses.
#   2. No hard-coded secrets - DB credentials are read from the repo root .env
#      (gitignored) or from environment variables.
#   3. Does not modify docker-compose.yml or any service code.
#   4. Local/demo only - never run against a production environment.
#   5. Idempotent - safe to run repeatedly; users/data are never duplicated.
#   6. API-based seeding through the API Gateway where a route exists.
#   7. Direct SQL is used ONLY for the ADMIN role promotion of 'sunny' (the
#      Auth Service has no admin-role API) and for final verification counts.
#   8. Seeding only starts after the gateway is reachable, i.e. after JPA
#      (ddl-auto: update) has created all tables.
#   9. Existing Docker test accounts are never touched or deleted.
#  10. One common demo password for all demo users: Voltaras@123
#
# Usage (from the repository root):
#     bash docker/seed/seed-docker-demo-data.sh
#
# Optional environment variables:
#     GATEWAY_URL       http://localhost:8080  (API Gateway)
#     METER_MGMT_URL    http://localhost:8089  (meter-management-service,
#                                               no gateway route exists yet)
#     MYSQL_CONTAINER   voltaras-mysql         (docker container name)
#     ENV_FILE          .env                   (repo root environment file)
#     DEMO_READING_DATE 2026-07-15             (fixed past demo billing month)
# ============================================================================

set -uo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
METER_MGMT_URL="${METER_MGMT_URL:-http://localhost:8089}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-voltaras-mysql}"
ENV_FILE="${ENV_FILE:-}"
DEMO_READING_DATE="${DEMO_READING_DATE:-2026-07-15}"

COMMON_PASSWORD="Voltaras@123"
EMAIL_DOMAIN="voltaras.local"
SEED_TAG="[voltaras-demo]"
ORG_CODE="VOLTARAS_DEMO"
ORG_NAME="Voltaras Demo Society"
ADMIN_NAME="sunny"

NAMES=(soumya anil vinay pavan tarun bharath satya srivalli rekha sunny \
       uday sunil jash nagesh swaraj kavya rahul sneha kiran deepak \
       lavanya rohit meena akhil divya manoj priya charan harika naveen)

CITY="Hyderabad"
STATE="Telangana"
COUNTRY="India"

# Hyderabad / Telangana style demo addresses (cycled deterministically).
ADDR_LINES=("Flat 302, Sri Sai Residency, Jubilee Hills" \
            "H.No 12-34/5, Madhapur Main Road" \
            "Plot 45, Ayyappa Society, KPHB Colony" \
            "8-2-293/82/A, Road No 7, Banjara Hills" \
            "H.No 1-98/9/3, Kondapur" \
            "Flat 12A, Orchid Enclave, Gachibowli" \
            "2nd Floor, Sunrise Towers, Ameerpet" \
            "H.No 5-9-22, Secretariat Road, Secunderabad" \
            "Plot 88, SBI Colony, Uppal" \
            "H.No 6-3-248, Road No 1, Somajiguda")
PINS=(500033 500081 500072 500034 500084 500032 500016 500003 500039 500082)

# ---------------------------------------------------------------------------
# DB credentials from repo root .env (gitignored) or environment variables
# ---------------------------------------------------------------------------
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
[ -z "$ENV_FILE" ] && ENV_FILE="$REPO_ROOT/.env"

DB_USERNAME="${DB_USERNAME:-}"
DB_PASSWORD="${DB_PASSWORD:-}"
if [ -z "$DB_USERNAME" ] || [ -z "$DB_PASSWORD" ]; then
    if [ -f "$ENV_FILE" ]; then
        DB_USERNAME="$(grep -E '^DB_USERNAME=' "$ENV_FILE" | tail -1 | cut -d= -f2- | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
        DB_PASSWORD="$(grep -E '^DB_PASSWORD=' "$ENV_FILE" | tail -1 | cut -d= -f2- | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    fi
fi
if [ -z "$DB_USERNAME" ] || [ -z "$DB_PASSWORD" ]; then
    echo "ERROR: DB_USERNAME / DB_PASSWORD not found. Export them or provide the repo root .env file." >&2
    exit 1
fi

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
# Calls curl; sets HTTP_CODE and HTTP_BODY globals.
api() { # method url body token [extra-header-string]
    local method="$1" url="$2" body="${3:-}" token="${4:-}" extra="${5:-}"
    local args=(-s -w '\n%{http_code}' -X "$method" -H 'Content-Type: application/json')
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$extra" ] && args+=(-H "$extra")
    [ -n "$body" ] && args+=(-d "$body")

    local out
    out="$(curl "${args[@]}" "$url")"
    # Git Bash pipes translate \n to \r\n; strip \r so HTTP_CODE/HTTP_BODY
    # and every later JSON extraction stay clean.
    HTTP_CODE="$(printf '%s\n' "$out" | tail -1 | tr -d '\r')"
    HTTP_BODY="$(printf '%s\n' "$out" | sed '$d' | tr -d '\r')"
}

# Same as api(), but for direct meter-management-service calls (no gateway
# route exists for /api/meters/**). The service trusts X-User-Id / X-User-Role
# headers injected by the gateway in normal operation; here we inject them
# explicitly for local demo seeding only.
meter_api() {
    local method="$1" url="$2" body="${3:-}" xuid="$4" xrole="${5:-ADMIN}"
    local args=(-s -w '\n%{http_code}' -X "$method" -H 'Content-Type: application/json' \
        -H "X-User-Id: $xuid" -H "X-User-Role: $xrole")
    [ -n "$body" ] && args+=(-d "$body")

    local out
    out="$(curl "${args[@]}" "$url")"
    HTTP_CODE="$(printf '%s\n' "$out" | tail -1 | tr -d '\r')"
    HTTP_BODY="$(printf '%s\n' "$out" | sed '$d' | tr -d '\r')"
}

# Reads a dotted path from the JSON supplied on stdin.
json_get() { # expr  (JSON comes via stdin: json_get role <<<"$json")
    local expr="$1"
    python -c "
import sys, json
raw = sys.stdin.read()
try:
    d = json.loads(raw)
except Exception:
    sys.exit(1)
cur = d
for k in '$expr'.split('.'):
    if isinstance(cur, list):
        cur = cur[int(k)]
    else:
        cur = cur.get(k)
    if cur is None:
        sys.exit(1)
print(cur)
" 2>/dev/null || true
}

login() { # email -> sets LOGIN_JSON (no stdout)
    local email="$1"
    local body="{\"email\":\"$email\",\"password\":\"$COMMON_PASSWORD\"}"
    api POST "$GATEWAY_URL/api/auth/login" "$body"
    LOGIN_JSON="$HTTP_BODY"
}

ensure_user() { # name email phone address -> sets U_LOGIN (login json)
    local name="$1" email="$2" phone="$3" address="$4"
    local tok full
    # login() runs in the current shell so LOGIN_JSON / HTTP_BODY stay visible.
    login "$email"
    tok="$(json_get accessToken <<<"$HTTP_BODY")"
    if [ -n "$tok" ]; then
        echo "  [exists] $email"
    else
        full="$(python -c "print('$name'.capitalize())")"
        local body="{\"fullName\":\"$full\",\"email\":\"$email\",\"phone\":\"$phone\",\"password\":\"$COMMON_PASSWORD\",\"confirmPassword\":\"$COMMON_PASSWORD\",\"address\":\"$address\"}"
        api POST "$GATEWAY_URL/api/auth/register" "$body"
        if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "409" ]; then
            echo "  [created] $email"
        else
            echo "  [register failed $HTTP_CODE] $email" >&2
        fi
        login "$email"
    fi
    U_LOGIN="$LOGIN_JSON"
}

ensure_profile() { # userId fullName phone token
    local uid="$1" full="$2" phone="$3" token="$4"
    local addr_idx=$(( (uid - 1) % 10 ))
    local body="{\"fullName\":\"$full\",\"phone\":\"$phone\",\"address\":\"${ADDR_LINES[$addr_idx]}\",\"city\":\"$CITY\",\"state\":\"$STATE\",\"country\":\"$COUNTRY\",\"postalCode\":\"${PINS[$addr_idx]}\"}"
    api POST "$GATEWAY_URL/api/users/profile" "$body" "$token"
    # 201 = created, 409 = already exists (idempotent re-run) -> both fine
}

mysql_q() { # db sql -> stdout
    local db="$1" sql="$2"
    # -e is required so MYSQL_PWD reaches the container (host env is not
    # inherited by docker exec).
    docker exec -e MYSQL_PWD="$DB_PASSWORD" "$MYSQL_CONTAINER" \
        mysql -h127.0.0.1 -u"$DB_USERNAME" --batch --skip-column-names -D "$db" -e "$sql" 2>/dev/null
}

title() { echo; echo "=== $1 ==="; }

# ---------------------------------------------------------------------------
# 1. Preflight: Docker + gateway
# ---------------------------------------------------------------------------
echo "=== VOLTARAS demo data seeder ==="
echo "Gateway  : $GATEWAY_URL"
echo "MeterMgmt: $METER_MGMT_URL"
echo ""

echo "Checking Docker containers..."
if ! docker compose ps >/dev/null 2>&1; then
    echo "ERROR: docker compose is not available / Docker is not running." >&2
    echo "Start Docker Desktop, then run 'docker compose up -d --build' first." >&2
    exit 1
fi
docker compose ps

echo "Waiting for API Gateway to become reachable..."
GATEWAY_UP=0
for _ in $(seq 1 60); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$GATEWAY_URL/actuator/health")"
    if [ "$code" = "200" ]; then GATEWAY_UP=1; break; fi
    sleep 5
done
if [ "$GATEWAY_UP" != "1" ]; then
    echo "ERROR: API Gateway not reachable at $GATEWAY_URL after 5 minutes." >&2
    exit 1
fi
echo "API Gateway is UP."

# ---------------------------------------------------------------------------
# 2. Create / login the 30 demo users
# ---------------------------------------------------------------------------
title "Step 1/7: Creating 30 demo users"

declare -a USER_IDS USER_TOKENS USER_EMAILS USER_NAMES USER_PHONES
for i in "${!NAMES[@]}"; do
    name="${NAMES[$i]}"
    email="$name.demo@$EMAIL_DOMAIN"
    phone=$(( 9000000000 + i * 17 ))
    idx=$(( i % 10 ))
    address="${ADDR_LINES[$idx]}, $CITY, $STATE - ${PINS[$idx]}"

    echo "  user $((i+1))/30: $email"
    ensure_user "$name" "$email" "$phone" "$address"

    USER_IDS[$i]="$(json_get userId <<<"$U_LOGIN")"
    USER_TOKENS[$i]="$(json_get accessToken <<<"$U_LOGIN")"
    USER_EMAILS[$i]="$email"
    USER_NAMES[$i]="$name"
    USER_PHONES[$i]="$phone"
done

# ---------------------------------------------------------------------------
# 3. Promote 'sunny' to ADMIN (no auth API exists -> direct SQL on auth_db)
# ---------------------------------------------------------------------------
title "Step 2/7: Promoting '$ADMIN_NAME' to ADMIN"

ADMIN_IDX=-1
for i in "${!NAMES[@]}"; do
    if [ "${NAMES[$i]}" = "$ADMIN_NAME" ]; then ADMIN_IDX=$i; fi
done
SUNNY_ID="$(mysql_q auth_db "SELECT id FROM users WHERE email='${USER_EMAILS[$ADMIN_IDX]}'")"
CURRENT_ROLE="$(mysql_q auth_db "SELECT r.name FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=$SUNNY_ID")"
if [ "$CURRENT_ROLE" != "ADMIN" ]; then
    ADMIN_ROLE_ID="$(mysql_q auth_db "SELECT id FROM roles WHERE name='ADMIN'")"
    mysql_q auth_db "UPDATE user_roles ur SET ur.role_id=$ADMIN_ROLE_ID WHERE ur.user_id=$SUNNY_ID" >/dev/null
    login "${USER_EMAILS[$ADMIN_IDX]}"
    USER_TOKENS[$ADMIN_IDX]="$(json_get accessToken <<<"$LOGIN_JSON")"
    echo "  ${USER_EMAILS[$ADMIN_IDX]} promoted to ADMIN (user_id=$SUNNY_ID)."
else
    login "${USER_EMAILS[$ADMIN_IDX]}"
    USER_TOKENS[$ADMIN_IDX]="$(json_get accessToken <<<"$LOGIN_JSON")"
    echo "  ${USER_EMAILS[$ADMIN_IDX]} is already ADMIN."
fi
ADMIN_TOKEN="${USER_TOKENS[$ADMIN_IDX]}"

# ---------------------------------------------------------------------------
# 4. User profiles
# ---------------------------------------------------------------------------
title "Step 3/7: Creating user profiles"
for i in "${!NAMES[@]}"; do
    full="$(python -c "print('${NAMES[$i]}'.capitalize())")"
    ensure_profile "${USER_IDS[$i]}" "$full" "${USER_PHONES[$i]}" "${USER_TOKENS[$i]}"
done
echo "  profiles ensured for all 30 users."

# ---------------------------------------------------------------------------
# 5. Organization + memberships (owner = sunny)
# ---------------------------------------------------------------------------
title "Step 4/7: Demo organization + memberships"

ORG_ID="$(mysql_q organization_db "SELECT id FROM organizations WHERE organization_code='$ORG_CODE'")"
if [ -z "$ORG_ID" ]; then
    body="{\"name\":\"$ORG_NAME\",\"organizationCode\":\"$ORG_CODE\",\"organizationType\":\"APARTMENT\",\"description\":\"Residential electricity service organization for Hyderabad consumers.\",\"email\":\"demo.society@$EMAIL_DOMAIN\",\"phone\":\"9000000000\",\"addressLine1\":\"Plot 12, Road No 2, Kukatpally Housing Board\",\"city\":\"$CITY\",\"state\":\"$STATE\",\"country\":\"$COUNTRY\",\"postalCode\":\"500072\"}"
    api POST "$GATEWAY_URL/api/organizations" "$body" "$ADMIN_TOKEN"
    ORG_ID="$(json_get id <<<"$HTTP_BODY")"
    echo "  organization created: $ORG_CODE (id=$ORG_ID)"
else
    echo "  organization exists: $ORG_CODE (id=$ORG_ID)"
fi

for i in "${!NAMES[@]}"; do
    [ "${USER_EMAILS[$i]}" = "${USER_EMAILS[$ADMIN_IDX]}" ] && continue
    body="{\"requestedRole\":\"MEMBER\",\"requestMessage\":\"$SEED_TAG Join request for demo data.\"}"
    api POST "$GATEWAY_URL/api/organizations/$ORG_ID/join-requests" "$body" "${USER_TOKENS[$i]}"
done

api GET "$GATEWAY_URL/api/organizations/$ORG_ID/join-requests?status=PENDING" "" "$ADMIN_TOKEN"
PENDING_IDS="$(python -c "
import sys, json
raw = sys.stdin.read()
try:
    d = json.loads(raw)
except Exception:
    d = []
for r in (d if isinstance(d, list) else []):
    print(r.get('id'))
" <<<"$HTTP_BODY" | tr -d '\r')"
APPROVED=0
for rid in $PENDING_IDS; do
    api PATCH "$GATEWAY_URL/api/organizations/$ORG_ID/join-requests/$rid/approve" "" "$ADMIN_TOKEN"
    APPROVED=$((APPROVED+1))
done
echo "  $APPROVED pending join requests approved; $(( ${#NAMES[@]} - 1 )) consumers are members."

# ---------------------------------------------------------------------------
# 6. Meters (meter-management-service - no gateway route, direct call)
# ---------------------------------------------------------------------------
title "Step 5/7: Creating 30 meters and assigning them"

INSTALL_DATE="$(python -c "
import datetime
d = datetime.date.fromisoformat('$DEMO_READING_DATE')
print((d - datetime.timedelta(days=180)).isoformat())
")"

declare -a METER_IDS METER_NUMS
for i in "${!NAMES[@]}"; do
    mtr_no="$(printf 'MTR-DEMO-%04d' $((i+1)))"
    idx=$(( i % 10 ))
    body="{\"meterNumber\":\"$mtr_no\",\"meterType\":\"SMART\",\"connectionType\":\"RESIDENTIAL\",\"phaseType\":\"SINGLE_PHASE\",\"status\":\"ACTIVE\",\"sanctionedLoadKw\":5.0,\"installationDate\":\"$INSTALL_DATE\",\"addressLine\":\"${ADDR_LINES[$idx]}\",\"city\":\"$CITY\",\"state\":\"$STATE\",\"pincode\":\"${PINS[$idx]}\",\"remarks\":\"$SEED_TAG Seeded demo meter\"}"
    meter_api POST "$METER_MGMT_URL/api/meters/admin" "$body" "${USER_IDS[$ADMIN_IDX]}"
    MID="$(json_get id <<<"$HTTP_BODY")"
    if [ -z "$MID" ] && [ "$HTTP_CODE" = "409" ]; then
        # Already exists -> reuse the existing meter.
        meter_api GET "$METER_MGMT_URL/api/meters/admin?meterNumber=$mtr_no" "" "${USER_IDS[$ADMIN_IDX]}"
        MID="$(python -c "
import sys, json
d = json.loads(sys.stdin.read())
print(d[0]['id'] if isinstance(d, list) and d else '')
" <<<"$HTTP_BODY")"
    fi
    METER_IDS[$i]="$MID"
    METER_NUMS[$i]="$mtr_no"

    if [ -n "$MID" ]; then
        assign="{\"authUserId\":${USER_IDS[$i]},\"organizationId\":$ORG_ID}"
        meter_api PATCH "$METER_MGMT_URL/api/meters/admin/$MID/assign" "$assign" "${USER_IDS[$ADMIN_IDX]}"
        echo "  meter $mtr_no -> user ${USER_EMAILS[$i]} (id=$MID)"
    fi
done

# ---------------------------------------------------------------------------
# 7. Meter readings (submit as consumer, verify as admin)
# ---------------------------------------------------------------------------
title "Step 6/7: 30 meter readings (submitted + verified)"

declare -a READING_IDS READING_PREV READING_CURR
for i in "${!NAMES[@]}"; do
    previous=$(( 1000 + i * 150 ))
    current=$(( previous + 60 + i * 5 ))
    body="{\"meterNumber\":\"${METER_NUMS[$i]}\",\"previousReading\":$previous,\"currentReading\":$current,\"readingDate\":\"$DEMO_READING_DATE\",\"remarks\":\"$SEED_TAG Seeded demo reading\"}"
    api POST "$GATEWAY_URL/api/meter-readings" "$body" "${USER_TOKENS[$i]}"
    RID="$(json_get id <<<"$HTTP_BODY")"
    if [ -z "$RID" ] && [ "$HTTP_CODE" = "409" ]; then
        api GET "$GATEWAY_URL/api/meter-readings/me" "" "${USER_TOKENS[$i]}"
        RID="$(python -c "
import sys, json
d = json.loads(sys.stdin.read())
for r in d:
    if r.get('meterNumber') == '${METER_NUMS[$i]}' and r.get('readingDate') == '$DEMO_READING_DATE':
        print(r['id']); break
" <<<"$HTTP_BODY")"
    fi
    READING_IDS[$i]="$RID"
    READING_PREV[$i]="$previous"
    READING_CURR[$i]="$current"

    if [ -n "$RID" ]; then
        api PATCH "$GATEWAY_URL/api/meter-readings/admin/$RID/verify" "" "$ADMIN_TOKEN"
    fi
done
echo "  readings submitted and verified for all consumers."

# ---------------------------------------------------------------------------
# 8. Bills (admin generates one bill per consumer)
# ---------------------------------------------------------------------------
title "Step 7/7: 30 bills"

BILLING_MONTH="$(python -c "import datetime; print(datetime.date.fromisoformat('$DEMO_READING_DATE').month)")"
BILLING_YEAR="$(python -c "import datetime; print(datetime.date.fromisoformat('$DEMO_READING_DATE').year)")"
GEN_DATE="$(python -c "import datetime; d=datetime.date.fromisoformat('$DEMO_READING_DATE'); print((d.replace(day=1)+datetime.timedelta(days=35)).isoformat())")"
DUE_DATE="$(python -c "import datetime; d=datetime.date.fromisoformat('$DEMO_READING_DATE'); print((d.replace(day=1)+datetime.timedelta(days=55)).isoformat())")"

declare -a BILL_IDS BILL_TOTALS
for i in "${!NAMES[@]}"; do
    body="{\"authUserId\":${USER_IDS[$i]},\"meterReadingId\":${READING_IDS[$i]},\"meterNumber\":\"${METER_NUMS[$i]}\",\"previousReading\":${READING_PREV[$i]},\"currentReading\":${READING_CURR[$i]},\"billingMonth\":$BILLING_MONTH,\"billingYear\":$BILLING_YEAR,\"generatedDate\":\"$GEN_DATE\",\"dueDate\":\"$DUE_DATE\",\"remarks\":\"$SEED_TAG Seeded demo bill\"}"
    api POST "$GATEWAY_URL/api/bills/admin" "$body" "$ADMIN_TOKEN"
    BID="$(json_get id <<<"$HTTP_BODY")"
    TOTAL="$(json_get totalAmount <<<"$HTTP_BODY")"
    if [ -z "$BID" ] && [ "$HTTP_CODE" = "409" ]; then
        api GET "$GATEWAY_URL/api/bills/admin?month=$BILLING_MONTH&year=$BILLING_YEAR" "" "$ADMIN_TOKEN"
        read -r BID TOTAL <<<"$(python -c "
import sys, json
d = json.loads(sys.stdin.read())
for b in d:
    if b.get('authUserId') == ${USER_IDS[$i]}:
        print(b['id'], b['totalAmount']); break
" <<<"$HTTP_BODY")"
    fi
    BILL_IDS[$i]="$BID"
    BILL_TOTALS[$i]="$TOTAL"
done
echo "  bills generated for all 30 consumers."

# ---------------------------------------------------------------------------
# 9. Payments: wallet top-up (only if needed) + full bill payment
# ---------------------------------------------------------------------------
title "Bonus: wallet top-up + bill payments"

PAID=0
for i in "${!NAMES[@]}"; do
    [ -n "${BILL_IDS[$i]}" ] || continue
    api GET "$GATEWAY_URL/api/wallet/me" "" "${USER_TOKENS[$i]}"
    BALANCE="$(json_get balance <<<"$HTTP_BODY")"
    NEEDS_TOPUP="$(python -c "
try:
    b = float('$BALANCE' or 0); t = float('${BILL_TOTALS[$i]}' or 0)
    print(1 if b < t else 0)
except Exception:
    print(1)
")"
    if [ "$NEEDS_TOPUP" = "1" ]; then
        api POST "$GATEWAY_URL/api/wallet/top-up" '{"amount":3000.00}' "${USER_TOKENS[$i]}"
    fi
    idem_key="voltaras-demo-pay-${USER_IDS[$i]}-${BILL_IDS[$i]}"
    body="{\"amount\":${BILL_TOTALS[$i]},\"currency\":\"INR\",\"organizationId\":$ORG_ID}"
    api POST "$GATEWAY_URL/api/bills/${BILL_IDS[$i]}/payments" "$body" "${USER_TOKENS[$i]}" "Idempotency-Key: $idem_key"
    if [ "$HTTP_CODE" = "201" ]; then
        PAID=$((PAID+1))
    fi
done
echo "  $PAID / 30 bills paid from demo wallets."

# ---------------------------------------------------------------------------
# 10. Complaints (idempotent - a consumer never gets a second complaint)
# ---------------------------------------------------------------------------
title "Bonus: sample complaints"

COMPLAINTS=0
SUBJECT_K=0
# Fixed subset of 10 consumers (indices 0-8 plus 10; index 9 is the admin) so
# the step is strictly idempotent - re-runs never add complaints for new users.
# One realistic electricity-service subject per consumer with a matching
# category name (resolved against the live categories API) and a natural
# consumer-written description, kept in sync with the demo complaint records
# documented in docs/15_DOCKER_DEMO_DATA.md.
COMPLAINT_SUBJECTS=(
  "Incorrect billing amount"      # soumya
  "Meter reading mismatch"        # anil
  "Payment not reflected"         # vinay
  "Unexpected usage increase"     # pavan
  "Meter display issue"           # tarun
  "Bill due-date clarification"   # bharath
  "Wallet balance discrepancy"    # satya
  "Service interruption report"   # srivalli
  "Frequent voltage fluctuation"  # rekha
  "Meter not reporting readings"  # uday
)
COMPLAINT_CATEGORIES=(
  "BILLING_ISSUE"  # soumya
  "METER_ISSUE"    # anil
  "PAYMENT_ISSUE"  # vinay
  "METER_ISSUE"    # pavan
  "METER_ISSUE"    # tarun
  "BILLING_ISSUE"  # bharath
  "PAYMENT_ISSUE"  # satya
  "OTHER"          # srivalli
  "OTHER"          # rekha
  "METER_ISSUE"    # uday
)
COMPLAINT_DESCRIPTIONS=(
  "My electricity bill for this billing period is much higher than my actual usage. I have reviewed my previous bills and my consumption has not changed, so the amount seems incorrect. Please review the bill and correct it if there is an error."
  "The meter reading used for my latest bill does not match the reading shown on my meter display. My bill shows a higher reading than what I can see on the meter. Please recheck the reading and update my bill accordingly."
  "I paid my electricity bill through the wallet a few days ago, but the payment is still not showing on my account. The amount was deducted from my wallet but my bill still shows as unpaid. Please verify the transaction and update my bill status."
  "My electricity usage this month is much higher than usual even though my daily habits have not changed. I would like to understand why my consumption increased so much and confirm the reading is accurate."
  "The display on my electricity meter is not working properly. The screen is flickering and sometimes shows blank, so I cannot read my current usage. Please arrange an inspection of my meter."
  "I received my latest electricity bill later than usual and the due date seems very close. I want to confirm the exact due date so I can make the payment on time and avoid any late fee."
  "My wallet balance does not match my records. I believe a recharge I made earlier has not been credited, or a payment was deducted twice. Please check my wallet transactions and correct the balance."
  "I have been facing repeated interruptions in my electricity supply over the past few days. The power goes off without any prior notice and returns after some time. Please look into the supply issue in my area."
  "I am experiencing frequent voltage fluctuations at my home. The lights flicker and some appliances switch off suddenly. This has been happening for the past week. Please check the supply voltage in my area."
  "My electricity meter has not been reporting readings for the last two billing cycles, and my bill is being estimated instead of based on actual usage. Please check the meter connection and resolve this issue."
)
for i in 0 1 2 3 4 5 6 7 8 10; do
    # Resolve the category id for this complaint from the live categories API.
    api GET "$GATEWAY_URL/api/complaints/categories" "" "${USER_TOKENS[$i]}"
    CATEGORY_ID="$(python -c "
import sys, json
name = sys.argv[1]
d = json.loads(sys.stdin.read())
items = d if isinstance(d, list) else []
for c in items:
    if c.get('name') == name:
        print(c['id'])
        break
" "${COMPLAINT_CATEGORIES[$SUBJECT_K]}" <<<"$HTTP_BODY")"

    subject="${COMPLAINT_SUBJECTS[$SUBJECT_K]}"
    description="${COMPLAINT_DESCRIPTIONS[$SUBJECT_K]}"
    SUBJECT_K=$((SUBJECT_K+1))
    api GET "$GATEWAY_URL/api/complaints" "" "${USER_TOKENS[$i]}"
    ALREADY="$(python -c "
import sys, json
d = json.loads(sys.stdin.read())
items = d.get('content', d) if isinstance(d, dict) else d
print(1 if items else 0)
" <<<"$HTTP_BODY")"
    if [ "$ALREADY" != "1" ]; then
        body="{\"categoryId\":$CATEGORY_ID,\"subject\":\"$subject\",\"description\":\"$description\"}"
        api POST "$GATEWAY_URL/api/complaints" "$body" "${USER_TOKENS[$i]}"
        if [ "$HTTP_CODE" = "201" ]; then COMPLAINTS=$((COMPLAINTS+1)); fi
    fi
done
echo "  $COMPLAINTS complaints created (demo only)."

# ---------------------------------------------------------------------------
# 11. Verification
# ---------------------------------------------------------------------------
title "Verification"

login "${USER_EMAILS[$ADMIN_IDX]}"
ADMIN_LOGIN="$LOGIN_JSON"
login "vinay.demo@$EMAIL_DOMAIN"
VINAY_LOGIN="$LOGIN_JSON"
ADMIN_ROLE="$(json_get role <<<"$ADMIN_LOGIN")"
VINAY_ROLE="$(json_get role <<<"$VINAY_LOGIN")"
VINAY_TOKEN="$(json_get accessToken <<<"$VINAY_LOGIN")"
echo "  login sunny (ADMIN)    -> role=$ADMIN_ROLE userId=$(json_get userId <<<"$ADMIN_LOGIN")"
echo "  login vinay (CONSUMER) -> role=$VINAY_ROLE userId=$(json_get userId <<<"$VINAY_LOGIN")"

api GET "$GATEWAY_URL/api/complaints" "" "$VINAY_TOKEN"
VINAY_COUNT="$(python -c "
import sys, json
d = json.loads(sys.stdin.read())
items = d.get('content', d) if isinstance(d, dict) else d
print(len(items) if isinstance(items, list) else 0)
" <<<"$HTTP_BODY")"
api GET "$GATEWAY_URL/api/admin/complaints" "" "$ADMIN_TOKEN"
ADMIN_COUNT="$(python -c "
import sys, json
d = json.loads(sys.stdin.read())
print(d.get('totalElements', 0) if isinstance(d, dict) else len(d))
" <<<"$HTTP_BODY")"
echo "  GET /api/complaints (vinay)       -> $VINAY_COUNT complaints"
echo "  GET /api/admin/complaints (sunny) -> $ADMIN_COUNT complaints"

echo ""
echo "Database counts (demo users only, email like '%.demo@$EMAIL_DOMAIN'):"
count() { # label db sql
    printf "  %-32s = %s\n" "$1" "$(mysql_q "$2" "$3")"
}
count "users (auth_db)" auth_db "SELECT COUNT(*) FROM users WHERE email LIKE '%.demo@$EMAIL_DOMAIN'"
count "ADMIN users" auth_db "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.email LIKE '%.demo@$EMAIL_DOMAIN' AND r.name='ADMIN'"
count "CONSUMER users" auth_db "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.email LIKE '%.demo@$EMAIL_DOMAIN' AND r.name='CONSUMER'"
count "user profiles (user_db)" user_db "SELECT COUNT(*) FROM user_profiles p JOIN auth_db.users u ON u.id=p.auth_user_id WHERE u.email LIKE '%.demo@$EMAIL_DOMAIN'"
count "organizations" organization_db "SELECT COUNT(*) FROM organizations WHERE organization_code='$ORG_CODE'"
count "organization memberships" organization_db "SELECT COUNT(*) FROM organization_memberships m JOIN organizations o ON o.id=m.organization_id WHERE o.organization_code='$ORG_CODE'"
count "meters" meter_management_db "SELECT COUNT(*) FROM meters WHERE meter_number LIKE 'MTR-DEMO-%'"
count "meter readings" meter_db "SELECT COUNT(*) FROM meter_readings WHERE remarks LIKE '%$SEED_TAG%'"
count "bills" bill_db "SELECT COUNT(*) FROM bills WHERE remarks LIKE '%$SEED_TAG%'"
count "payments" payment_db "SELECT COUNT(*) FROM payments"
count "complaints" complaint_db "SELECT COUNT(*) FROM complaints WHERE subject IN ('Incorrect billing amount','Meter reading mismatch','Payment not reflected','Unexpected usage increase','Meter display issue','Bill due-date clarification','Wallet balance discrepancy','Service interruption report','Frequent voltage fluctuation','Meter not reporting readings')"

echo ""
echo "=== Demo data seeding finished ==="
echo "Common password : $COMMON_PASSWORD"
echo "Admin login     : ${USER_EMAILS[$ADMIN_IDX]}  /  $COMMON_PASSWORD"
echo "Consumer login  : vinay.demo@$EMAIL_DOMAIN  /  $COMMON_PASSWORD"
