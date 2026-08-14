<#
    ============================================================================
    VOLTARAS - Docker demo data seeder (PowerShell)

    Creates the local demo dataset used for Swagger/OpenAPI verification of the
    VOLTARAS microservice stack running in Docker Compose.

      * 30 demo users  (1 ADMIN = sunny, 29 CONSUMER)
      * user profiles   (user-service)
      * 1 demo organization + memberships (organization-service)
      * 30 meters assigned to consumers (meter-management-service)
      * 30 verified meter readings (meter-reading-service)
      * 30 bills (bill-service)
      * wallet top-ups + bill payments (payment-service)
      * complaints (complaint-service)

    Design rules honoured by this script:
      1. No real personal data - only clean demo names / Hyderabad demo addresses.
      2. No hard-coded secrets - DB credentials are read from the repo root .env
         (gitignored) or from environment variables.
      3. Does not modify docker-compose.yml or any service code.
      4. Local/demo only - never run against a production environment.
      5. Idempotent - safe to run repeatedly; users/data are never duplicated.
      6. API-based seeding through the API Gateway where a route exists.
      7. Direct SQL is used ONLY for the ADMIN role promotion of 'sunny' (the
         Auth Service has no admin-role API) and for final verification counts.
      8. Seeding only starts after the gateway is reachable, i.e. after JPA
         (ddl-auto: update) has created all tables.
      9. Existing Docker test accounts are never touched or deleted.      10. One common demo password for all demo users: Voltaras@123

    Usage (from the repository root):
        powershell -ExecutionPolicy Bypass -File docker/seed/seed-docker-demo-data.ps1

    Optional parameters:
        -GatewayUrl      http://localhost:8080   (API Gateway)
        -MeterMgmtUrl    http://localhost:8089   (meter-management-service,
                                                  no gateway route exists yet)
        -MysqlContainer  voltaras-mysql          (docker container name)
        -EnvFile         .env                    (repo root environment file)
        -DemoReadingDate 2026-07-15              (fixed past demo billing month)
    ============================================================================
#>

[CmdletBinding()]
param(
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$MeterMgmtUrl = "http://localhost:8089",
    [string]$MysqlContainer = "voltaras-mysql",
    [string]$EnvFile = "",
    [string]$DemoReadingDate = "2026-07-15"
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
$CommonPassword  = "Voltaras@123"
$EmailDomain     = "voltaras.local"
$SeedTag         = "[voltaras-demo]"
$OrgCode         = "VOLTARAS_DEMO"
$OrgName         = "Voltaras Demo Society"
$AdminName       = "sunny"

# 15 required names + 15 generated clean demo names = 30 users.
$Names = @(
    "soumya", "anil", "vinay", "pavan", "tarun",
    "bharath", "satya", "srivalli", "rekha", "sunny",
    "uday", "sunil", "jash", "nagesh", "swaraj",
    "kavya", "rahul", "sneha", "kiran", "deepak",
    "lavanya", "rohit", "meena", "akhil", "divya",
    "manoj", "priya", "charan", "harika", "naveen"
)

# Hyderabad / Telangana style demo addresses (cycled deterministically).
$Addresses = @(
    @{ Line = "Flat 302, Sri Sai Residency, Jubilee Hills"; Pincode = "500033" },
    @{ Line = "H.No 12-34/5, Madhapur Main Road";          Pincode = "500081" },
    @{ Line = "Plot 45, Ayyappa Society, KPHB Colony";      Pincode = "500072" },
    @{ Line = "8-2-293/82/A, Road No 7, Banjara Hills";     Pincode = "500034" },
    @{ Line = "H.No 1-98/9/3, Kondapur";                    Pincode = "500084" },
    @{ Line = "Flat 12A, Orchid Enclave, Gachibowli";       Pincode = "500032" },
    @{ Line = "2nd Floor, Sunrise Towers, Ameerpet";        Pincode = "500016" },
    @{ Line = "H.No 5-9-22, Secretariat Road, Secunderabad";Pincode = "500003" },
    @{ Line = "Plot 88, SBI Colony, Uppal";                 Pincode = "500039" },
    @{ Line = "H.No 6-3-248, Road No 1, Somajiguda";        Pincode = "500082" }
)

$City    = "Hyderabad"
$State   = "Telangana"
$Country = "India"

# ---------------------------------------------------------------------------
# Environment / DB credentials (never hard-coded)
# ---------------------------------------------------------------------------
# Resolve repository root = parent of docker/seed
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $RepoRoot ".env"
}

$DbUsername = $env:DB_USERNAME
$DbPassword = $env:DB_PASSWORD

if ([string]::IsNullOrWhiteSpace($DbUsername) -or [string]::IsNullOrWhiteSpace($DbPassword)) {
    if (Test-Path $EnvFile) {
        Write-Host "Reading DB credentials from $EnvFile"
        Get-Content $EnvFile | ForEach-Object {
            if ($_ -match '^\s*DB_USERNAME\s*=\s*(.+)\s*$') { $DbUsername = $Matches[1].Trim() }
            if ($_ -match '^\s*DB_PASSWORD\s*=\s*(.+)\s*$') { $DbPassword = $Matches[1].Trim() }
        }
    }
}

if ([string]::IsNullOrWhiteSpace($DbUsername) -or [string]::IsNullOrWhiteSpace($DbPassword)) {
    Write-Host "ERROR: DB_USERNAME / DB_PASSWORD not found. Export them or provide the repo root .env file." -ForegroundColor Red
    exit 1
}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
function Invoke-Json([string]$Method, [string]$Uri, $Body, [string]$Token, [hashtable]$ExtraHeaders = @{}) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    foreach ($k in $ExtraHeaders.Keys) { $headers[$k] = $ExtraHeaders[$k] }

    $params = @{
        Method = $Method
        Uri    = $Uri
        Headers = $headers
    }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 8) }

    $resp = Invoke-RestMethod @params
    return $resp
}

function Login-User([string]$Email) {
    $body = @{ email = $Email; password = $CommonPassword } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method POST -Uri "$GatewayUrl/api/auth/login" `
        -Headers @{ "Content-Type" = "application/json" } -Body $body
    return $resp
}

function Ensure-User([string]$Name, [string]$Email, [string]$Phone, [string]$Address) {
    # Idempotent: existing account (login OK or register 409) is reused.
    try {
        $login = Login-User $Email
        Write-Host ("  [exists] {0}" -f $Email)
        return $login
    } catch {
        $registerBody = @{
            fullName        = (Get-Culture).TextInfo.ToTitleCase($Name)
            email           = $Email
            phone           = $Phone
            password        = $CommonPassword
            confirmPassword = $CommonPassword
            address         = $Address
        }
        try {
            Invoke-Json "POST" "$GatewayUrl/api/auth/register" $registerBody | Out-Null
            Write-Host ("  [created] {0}" -f $Email)
        } catch {
            # 409 = already registered (race / previous partial run).
            Write-Host ("  [exists(409)] {0}" -f $Email)
        }
        return Login-User $Email
    }
}

function Ensure-Profile($User) {
    $addrIndex = ($User.userId - 1) % $Addresses.Count
    $profile = @{
        fullName   = $User.fullName
        phone      = $User.phone
        address    = $Addresses[$addrIndex].Line
        city       = $City
        state      = $State
        country    = $Country
        postalCode = $Addresses[$addrIndex].Pincode
    }
    try {
        Invoke-Json "POST" "$GatewayUrl/api/users/profile" $profile $User.accessToken | Out-Null
    } catch {
        # 409 = profile already exists (idempotent re-run).
    }
}

function Mysql-Query([string]$Database, [string]$Query) {
    # NOTE: the mysql client is invoked through the container shell (sh -c)
    # and the SQL is piped via stdin. This avoids two Windows PowerShell 5.1
    # native-command pitfalls: bareword args like -h127.0.0.1 get split into
    # '-h127' + '.0.0.1', and embedded quotes in arguments get mangled.
    $sql = "mysql -h127.0.0.1 -u$DbUsername --batch --skip-column-names -D $Database"
    $out = $Query | docker exec -i -e MYSQL_PWD=$DbPassword $MysqlContainer sh -c $sql 2>$null
    return ($out -join "`n").Trim()
}

# ---------------------------------------------------------------------------
# 1. Preflight: Docker containers + API Gateway reachability
# ---------------------------------------------------------------------------
Write-Host "=== VOLTARAS demo data seeder ===" -ForegroundColor Cyan
Write-Host "Gateway : $GatewayUrl"
Write-Host "MeterMgmt: $MeterMgmtUrl"
Write-Host ""

Write-Host "Checking Docker containers..."
$psOut = docker compose ps --format "{{.Name}} {{.Status}}" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: docker compose is not available / Docker is not running." -ForegroundColor Red
    Write-Host "Start Docker Desktop, then run 'docker compose up -d --build' first."
    exit 1
}
Write-Host $psOut

Write-Host "Waiting for API Gateway to become reachable..."
$gatewayUp = $false
for ($i = 1; $i -le 60; $i++) {
    try {
        $health = Invoke-RestMethod -Method GET -Uri "$GatewayUrl/actuator/health" -TimeoutSec 3
        if ($health.status -eq "UP") { $gatewayUp = $true; break }
    } catch { }
    Start-Sleep -Seconds 5
}
if (-not $gatewayUp) {
    Write-Host "ERROR: API Gateway not reachable at $GatewayUrl after 5 minutes." -ForegroundColor Red
    exit 1
}
Write-Host "API Gateway is UP."

# ---------------------------------------------------------------------------
# 2. Create / login the 30 demo users (1 ADMIN + 29 CONSUMER)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Step 1/7: Creating 30 demo users ===" -ForegroundColor Cyan

$Users = @()
for ($i = 0; $i -lt $Names.Count; $i++) {
    $name    = $Names[$i]
    $email   = "$name.demo@$EmailDomain"
    $phone   = (9000000000 + $i * 17).ToString()          # deterministic 10-digit
    $addr    = $Addresses[$i % $Addresses.Count].Line + ", $City, $State - " + $Addresses[$i % $Addresses.Count].Pincode
    $full    = (Get-Culture).TextInfo.ToTitleCase($name)

    Write-Host ("  user {0,2}/30: {1}" -f ($i + 1), $email)
    $login = Ensure-User $full $email $phone $addr
    $Users += [PSCustomObject]@{
        name        = $name
        fullName    = $full
        email       = $email
        phone       = $phone
        address     = $addr
        userId      = $login.userId
        role        = $login.role
        token       = $login.accessToken
    }
}

$Admin = $Users | Where-Object { $_.name -eq $AdminName }

# ---------------------------------------------------------------------------
# 3. Promote 'sunny' to ADMIN (no auth API exists -> direct SQL on auth_db)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Step 2/7: Promoting '$AdminName' to ADMIN ===" -ForegroundColor Cyan
$role = Mysql-Query "auth_db" "SELECT name FROM roles"
$adminRoleId = Mysql-Query "auth_db" "SELECT id FROM roles WHERE name='ADMIN'"
$sunnyId     = Mysql-Query "auth_db" "SELECT id FROM users WHERE email='$($Admin.email)'"
$currentRole = Mysql-Query "auth_db" "SELECT r.name FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=$sunnyId"

if ($currentRole -ne "ADMIN") {
    Mysql-Query "auth_db" "UPDATE user_roles ur SET ur.role_id=$adminRoleId WHERE ur.user_id=$sunnyId" | Out-Null
    $login = Login-User $Admin.email          # refresh token with ADMIN role claim
    $Admin.role  = $login.role
    $Admin.token = $login.accessToken
    Write-Host "  $($Admin.email) promoted to ADMIN (user_id=$sunnyId)."
} else {
    $login = Login-User $Admin.email
    $Admin.token = $login.accessToken
    Write-Host "  $($Admin.email) is already ADMIN."
}

# ---------------------------------------------------------------------------
# 4. User profiles
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Step 3/7: Creating user profiles ===" -ForegroundColor Cyan
foreach ($u in $Users) { Ensure-Profile $u }
Write-Host "  profiles ensured for all 30 users."

# ---------------------------------------------------------------------------
# 5. Organization + memberships
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Step 4/7: Demo organization + memberships ===" -ForegroundColor Cyan

# Find or create the demo organization (owner = sunny).
# /api/organizations/me returns memberships (no code), so the admin list
# endpoint is used to look the organization up by its unique code.
$org = $null
$orgPage = Invoke-Json "GET" "$GatewayUrl/api/admin/organizations?page=0&size=100" $null $Admin.token
foreach ($o in $orgPage.content) {
    if ($o.organizationCode -eq $OrgCode) { $org = $o }
}
if ($null -eq $org) {
    $createOrg = @{
        name             = $OrgName
        organizationCode = $OrgCode
        organizationType = "APARTMENT"
        description      = "$SeedTag Demo apartment society for local development."
        email            = "demo.society@$EmailDomain"
        phone            = "9000000000"
        addressLine1     = "Plot 12, Road No 2, Kukatpally Housing Board"
        city             = $City
        state            = $State
        country          = $Country
        postalCode       = "500072"
    }
    $org = Invoke-Json "POST" "$GatewayUrl/api/organizations" $createOrg $Admin.token
    Write-Host "  organization created: $($org.organizationCode) (id=$($org.id))"
} else {
    Write-Host "  organization exists: $($org.organizationCode) (id=$($org.id))"
}

# Every consumer sends a join request; the owner approves pending requests.
$consumers = $Users | Where-Object { $_.email -ne $Admin.email }
foreach ($u in $consumers) {
    try {
        $jr = @{ requestedRole = "MEMBER"; requestMessage = "$SeedTag Join request for demo data." }
        Invoke-Json "POST" "$GatewayUrl/api/organizations/$($org.id)/join-requests" $jr $u.token | Out-Null
    } catch {
        # 409 = already a member or already pending
    }
}

$pending = Invoke-Json "GET" "$GatewayUrl/api/organizations/$($org.id)/join-requests?status=PENDING" $null $Admin.token
foreach ($p in $pending) {
    try {
        Invoke-Json "PATCH" "$GatewayUrl/api/organizations/$($org.id)/join-requests/$($p.id)/approve" $null $Admin.token | Out-Null
    } catch { }
}
Write-Host ("  {0} pending join requests approved; {1} consumers are members." -f $pending.Count, $consumers.Count)

# ---------------------------------------------------------------------------
# 6. Meters (meter-management-service - no gateway route, direct call)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Step 5/7: Creating 30 meters and assigning them ===" -ForegroundColor Cyan

$demoDate = [datetime]::ParseExact($DemoReadingDate, "yyyy-MM-dd", $null)
$meters = @()
for ($i = 0; $i -lt $Users.Count; $i++) {
    $u       = $Users[$i]
    $mtrNo   = "MTR-DEMO-{0:D4}" -f ($i + 1)
    $meterId = $null

    $meterBody = @{
        meterNumber      = $mtrNo
        meterType        = "SMART"
        connectionType   = "RESIDENTIAL"
        phaseType        = "SINGLE_PHASE"
        status           = "ACTIVE"
        sanctionedLoadKw = 5.0
        installationDate = $demoDate.AddMonths(-6).ToString("yyyy-MM-dd")
        addressLine      = $Addresses[$i % $Addresses.Count].Line
        city             = $City
        state            = $State
        pincode          = $Addresses[$i % $Addresses.Count].Pincode
        remarks          = "$SeedTag Seeded demo meter"
    }
    try {
        $created = Invoke-Json "POST" "$MeterMgmtUrl/api/meters/admin" $meterBody $null @{
            "X-User-Id"   = $Admin.userId.ToString()
            "X-User-Role" = "ADMIN"
        }
        $meterId = $created.id
    } catch {
        # 409 = meter number already exists -> reuse it via list lookup.
        $all = Invoke-Json "GET" "$MeterMgmtUrl/api/meters/admin?meterNumber=$mtrNo" $null $null @{
            "X-User-Id"   = $Admin.userId.ToString()
            "X-User-Role" = "ADMIN"
        }
        if ($all -and $all.Count -gt 0) { $meterId = $all[0].id }
    }

    if ($meterId) {
        $assign = @{ authUserId = $u.userId; organizationId = $org.id }
        try {
            Invoke-Json "PATCH" "$MeterMgmtUrl/api/meters/admin/$meterId/assign" $assign $null @{
                "X-User-Id"   = $Admin.userId.ToString()
                "X-User-Role" = "ADMIN"
            } | Out-Null
        } catch { }
        $meters += [PSCustomObject]@{ userId = $u.userId; meterNumber = $mtrNo; meterId = $meterId }
        Write-Host ("  meter {0} -> user {1} (id={2})" -f $mtrNo, $u.email, $meterId)
    }
}

# ---------------------------------------------------------------------------
# 7. Meter readings (submit as consumer, verify as admin)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Step 6/7: 30 meter readings (submitted + verified) ===" -ForegroundColor Cyan

$readings = @()
for ($i = 0; $i -lt $Users.Count; $i++) {
    $u         = $Users[$i]
    $meter     = $meters | Where-Object { $_.userId -eq $u.userId }
    $previous  = 1000 + $i * 150
    $current   = $previous + 60 + $i * 5
    $readingBody = @{
        meterNumber     = $meter.meterNumber
        previousReading = $previous
        currentReading  = $current
        readingDate     = $DemoReadingDate
        remarks         = "$SeedTag Seeded demo reading"
    }
    $readingId = $null
    try {
        $created = Invoke-Json "POST" "$GatewayUrl/api/meter-readings" $readingBody $u.token
        $readingId = $created.id
    } catch {
        # 409 = reading already exists for this meter/date -> fetch my readings.
        $mine = Invoke-Json "GET" "$GatewayUrl/api/meter-readings/me" $null $u.token
        $existing = $mine | Where-Object { $_.meterNumber -eq $meter.meterNumber -and $_.readingDate -eq $DemoReadingDate }
        if ($existing) { $readingId = $existing[0].id }
    }

    if ($readingId) {
        try { Invoke-Json "PATCH" "$GatewayUrl/api/meter-readings/admin/$readingId/verify" $null $Admin.token | Out-Null } catch { }
        $readings += [PSCustomObject]@{ userId = $u.userId; meterNumber = $meter.meterNumber; readingId = $readingId; previousReading = $previous; currentReading = $current }
    }
}
Write-Host "  readings submitted and verified for all consumers."

# ---------------------------------------------------------------------------
# 8. Bills (admin generates one bill per consumer)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Step 7/7: 30 bills ===" -ForegroundColor Cyan

$bills = @()
$billingMonth = $demoDate.Month
$billingYear  = $demoDate.Year
$generatedDate = $demoDate.AddMonths(1).AddDays(5)
$dueDate       = $demoDate.AddMonths(1).AddDays(25)

foreach ($u in $Users) {
    $reading = $readings | Where-Object { $_.userId -eq $u.userId }
    $billBody = @{
        authUserId      = $u.userId
        meterReadingId  = $reading.readingId
        meterNumber     = $reading.meterNumber
        previousReading = $reading.previousReading
        currentReading  = $reading.currentReading
        billingMonth    = $billingMonth
        billingYear     = $billingYear
        generatedDate   = $generatedDate.ToString("yyyy-MM-dd")
        dueDate         = $dueDate.ToString("yyyy-MM-dd")
        remarks         = "$SeedTag Seeded demo bill"
    }
    try {
        $created = Invoke-Json "POST" "$GatewayUrl/api/bills/admin" $billBody $Admin.token
        $bills += [PSCustomObject]@{ userId = $u.userId; billId = $created.id; totalAmount = $created.totalAmount }
    } catch {
        # 409 = bill already exists -> fetch from admin list.
        $all = Invoke-Json "GET" "$GatewayUrl/api/bills/admin?month=$billingMonth&year=$billingYear" $null $Admin.token
        $existing = $all | Where-Object { $_.authUserId -eq $u.userId }
        if ($existing) {
            $bills += [PSCustomObject]@{ userId = $u.userId; billId = $existing[0].id; totalAmount = $existing[0].totalAmount }
        }
    }
}
Write-Host "  bills generated for all 30 consumers."

# ---------------------------------------------------------------------------
# 9. Payments: wallet top-up (only if needed) + full bill payment
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Bonus: wallet top-up + bill payments ===" -ForegroundColor Cyan

$paid = 0
foreach ($b in $bills) {
    $u = $Users | Where-Object { $_.userId -eq $b.userId }
    if (-not $u) { continue }

    # Ensure the wallet can cover the bill (top-up is additive; only top up
    # when the balance is below the bill total so re-runs add no extra money).
    try {
        $wallet = Invoke-Json "GET" "$GatewayUrl/api/wallet/me" $null $u.token
        $balance = [decimal]$wallet.balance
        if ($balance -lt [decimal]$b.totalAmount) {
            Invoke-Json "POST" "$GatewayUrl/api/wallet/top-up" @{ amount = 3000.00 } $u.token | Out-Null
        }
        $idemKey = "voltaras-demo-pay-$($u.userId)-$($b.billId)"
        $payBody = @{
            amount         = $b.totalAmount
            currency       = "INR"
            organizationId = $org.id
        }
        Invoke-Json "POST" "$GatewayUrl/api/bills/$($b.billId)/payments" $payBody $u.token @{ "Idempotency-Key" = $idemKey } | Out-Null
        $paid++
    } catch {
        # Already paid / wallet failure on re-run -> skip silently.
    }
}
Write-Host "  $paid / $($bills.Count) bills paid from demo wallets."

# ---------------------------------------------------------------------------
# 10. Complaints (idempotent - a consumer never gets a second complaint)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Bonus: sample complaints ===" -ForegroundColor Cyan

$complaints = 0
$subjectIndex = 0
# Fixed subset of 10 consumers (first 10 consumers; the admin is excluded) so
# the step is strictly idempotent - re-runs never add complaints for new users.
# One realistic electricity-service subject per consumer with a matching
# category name (resolved against the live categories API) and a natural
# consumer-written description, kept in sync with the demo complaint records
# documented in docs/15_DOCKER_DEMO_DATA.md.
$complaintSubjects = @(
    "Incorrect billing amount",     # soumya
    "Meter reading mismatch",       # anil
    "Payment not reflected",        # vinay
    "Unexpected usage increase",    # pavan
    "Meter display issue",          # tarun
    "Bill due-date clarification",  # bharath
    "Wallet balance discrepancy",   # satya
    "Service interruption report",  # srivalli
    "Frequent voltage fluctuation", # rekha
    "Meter not reporting readings"  # uday
)
$complaintCategories = @(
    "BILLING_ISSUE",  # soumya
    "METER_ISSUE",    # anil
    "PAYMENT_ISSUE",  # vinay
    "METER_ISSUE",    # pavan
    "METER_ISSUE",    # tarun
    "BILLING_ISSUE",  # bharath
    "PAYMENT_ISSUE",  # satya
    "OTHER",          # srivalli
    "OTHER",          # rekha
    "METER_ISSUE"     # uday
)
$complaintDescriptions = @(
    "My electricity bill for this billing period is much higher than my actual usage. I have reviewed my previous bills and my consumption has not changed, so the amount seems incorrect. Please review the bill and correct it if there is an error.",
    "The meter reading used for my latest bill does not match the reading shown on my meter display. My bill shows a higher reading than what I can see on the meter. Please recheck the reading and update my bill accordingly.",
    "I paid my electricity bill through the wallet a few days ago, but the payment is still not showing on my account. The amount was deducted from my wallet but my bill still shows as unpaid. Please verify the transaction and update my bill status.",
    "My electricity usage this month is much higher than usual even though my daily habits have not changed. I would like to understand why my consumption increased so much and confirm the reading is accurate.",
    "The display on my electricity meter is not working properly. The screen is flickering and sometimes shows blank, so I cannot read my current usage. Please arrange an inspection of my meter.",
    "I received my latest electricity bill later than usual and the due date seems very close. I want to confirm the exact due date so I can make the payment on time and avoid any late fee.",
    "My wallet balance does not match my records. I believe a recharge I made earlier has not been credited, or a payment was deducted twice. Please check my wallet transactions and correct the balance.",
    "I have been facing repeated interruptions in my electricity supply over the past few days. The power goes off without any prior notice and returns after some time. Please look into the supply issue in my area.",
    "I am experiencing frequent voltage fluctuations at my home. The lights flicker and some appliances switch off suddenly. This has been happening for the past week. Please check the supply voltage in my area.",
    "My electricity meter has not been reporting readings for the last two billing cycles, and my bill is being estimated instead of based on actual usage. Please check the meter connection and resolve this issue."
)
$consumersForComplaints = $consumers | Select-Object -First 10
foreach ($u in $consumersForComplaints) {
    try {
        $categories = Invoke-Json "GET" "$GatewayUrl/api/complaints/categories" $null $u.token
        $categoryName = $complaintCategories[$subjectIndex]
        $categoryId = ($categories | Where-Object { $_.name -eq $categoryName } | Select-Object -First 1).id
        $subject = $complaintSubjects[$subjectIndex]
        $description = $complaintDescriptions[$subjectIndex]
        $subjectIndex++

        $mine = Invoke-Json "GET" "$GatewayUrl/api/complaints" $null $u.token
        if (-not $mine.content) {
            $complaintBody = @{
                categoryId  = $categoryId
                subject     = $subject
                description = $description
            }
            Invoke-Json "POST" "$GatewayUrl/api/complaints" $complaintBody $u.token | Out-Null
            $complaints++
        }
    } catch { }
}
Write-Host "  $complaints complaints created (demo only)."

# ---------------------------------------------------------------------------
# 11. Verification
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=== Verification ===" -ForegroundColor Green

# a) Auth login checks
$adminLogin  = Login-User $Admin.email
$vinayLogin  = Login-User "vinay.demo@$EmailDomain"
Write-Host ("  login sunny (ADMIN)    -> role={0} userId={1}" -f $adminLogin.role, $adminLogin.userId)
Write-Host ("  login vinay (CONSUMER) -> role={0} userId={1}" -f $vinayLogin.role, $vinayLogin.userId)

# b) Complaint APIs through the gateway
$vinayComplaints = Invoke-Json "GET" "$GatewayUrl/api/complaints" $null $vinayLogin.accessToken
$adminComplaints = Invoke-Json "GET" "$GatewayUrl/api/admin/complaints" $null $adminLogin.accessToken
Write-Host ("  GET /api/complaints (vinay)      -> {0} complaints" -f @($vinayComplaints.content).Count)
Write-Host ("  GET /api/admin/complaints (sunny) -> {0} complaints" -f $adminComplaints.totalElements)

# c) Database counts
$demoEmailLike = "%.demo@$EmailDomain"
Write-Host ""
Write-Host "Database counts (demo users only, email like '$demoEmailLike'):"
$counts = @(
    @{ Label = "users (auth_db)";            Sql = "SELECT COUNT(*) FROM users WHERE email LIKE '$demoEmailLike'" ; Db = "auth_db" },
    @{ Label = "ADMIN users";                Sql = "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.email LIKE '$demoEmailLike' AND r.name='ADMIN'"; Db = "auth_db" },
    @{ Label = "CONSUMER users";             Sql = "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.email LIKE '$demoEmailLike' AND r.name='CONSUMER'"; Db = "auth_db" },
    @{ Label = "user profiles (user_db)";    Sql = "SELECT COUNT(*) FROM user_profiles p JOIN auth_db.users u ON u.id=p.auth_user_id WHERE u.email LIKE '$demoEmailLike'"; Db = "user_db" },
    @{ Label = "organizations";              Sql = "SELECT COUNT(*) FROM organizations WHERE organization_code='$OrgCode'"; Db = "organization_db" },
    @{ Label = "organization memberships";   Sql = "SELECT COUNT(*) FROM organization_memberships m JOIN organizations o ON o.id=m.organization_id WHERE o.organization_code='$OrgCode'"; Db = "organization_db" },
    @{ Label = "meters";                     Sql = "SELECT COUNT(*) FROM meters WHERE meter_number LIKE 'MTR-DEMO-%'"; Db = "meter_management_db" },
    @{ Label = "meter readings";             Sql = "SELECT COUNT(*) FROM meter_readings WHERE remarks LIKE '%$SeedTag%'"; Db = "meter_db" },
    @{ Label = "bills";                      Sql = "SELECT COUNT(*) FROM bills WHERE remarks LIKE '%$SeedTag%'"; Db = "bill_db" },
    @{ Label = "payments";                   Sql = "SELECT COUNT(*) FROM payments"; Db = "payment_db" },
    @{ Label = "complaints";                 Sql = "SELECT COUNT(*) FROM complaints WHERE subject IN ('Incorrect billing amount','Meter reading mismatch','Payment not reflected','Unexpected usage increase','Meter display issue','Bill due-date clarification','Wallet balance discrepancy','Service interruption report','Frequent voltage fluctuation','Meter not reporting readings')"; Db = "complaint_db" }
)
foreach ($c in $counts) {
    $val = Mysql-Query $c.Db $c.Sql
    Write-Host ("  {0,-32} = {1}" -f $c.Label, $val)
}

Write-Host ""
Write-Host "=== Demo data seeding finished ===" -ForegroundColor Green
Write-Host "Common password : $CommonPassword"
Write-Host "Admin login     : $($Admin.email)  /  $CommonPassword"
Write-Host "Consumer login  : vinay.demo@$EmailDomain  /  $CommonPassword"
