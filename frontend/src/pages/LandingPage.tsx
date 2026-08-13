import { Link } from 'react-router-dom'
import { ArrowRight, Gauge, Headphones, ReceiptText, Wallet, Zap } from 'lucide-react'
import heroImage from '@/assets/voltaras-transmission-grid-hero.webp'

const features = [
  {
    icon: Gauge,
    title: 'Smart metering',
    description:
      'Track your consumption with connected meters and verified monthly readings — no more estimated bills.',
  },
  {
    icon: ReceiptText,
    title: 'Transparent bills',
    description:
      'Clear, itemised bills generated from your actual usage, with due dates and statuses you can trust.',
  },
  {
    icon: Wallet,
    title: 'Instant payments',
    description:
      'Top up your wallet and pay bills in seconds with a secure, tracked payment history.',
  },
  {
    icon: Headphones,
    title: '24×7 support',
    description:
      'Raise complaints with a ticket number and follow every status change from open to resolved.',
  },
]

const steps = [
  {
    step: '01',
    title: 'Create your account',
    description: 'Register in under a minute with your name, phone and address.',
  },
  {
    step: '02',
    title: 'Connect your meter',
    description: 'Link your meter to start receiving verified readings and smart bills.',
  },
  {
    step: '03',
    title: 'Pay and track',
    description: 'Pay instantly, monitor usage and get support whenever you need it.',
  },
]

export function LandingPage() {
  return (
    <>
      {/* Hero — full-width electricity infrastructure backdrop with a solid navy
          overlay so the heading and actions stay readable; towers stay visible. */}
      <section className="relative overflow-hidden bg-navy-950">
        <img
          src={heroImage}
          alt="High-voltage electricity transmission towers against the sky"
          className="absolute inset-0 h-full w-full object-cover object-center"
        />
        <div aria-hidden="true" className="absolute inset-0 bg-navy-950/70" />
        <div className="relative mx-auto max-w-6xl px-4 py-20 sm:px-6 lg:py-28">
          <div className="max-w-xl">
            <p className="inline-flex items-center gap-2 rounded-full border border-volt-400/30 bg-volt-500/10 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-volt-200">
              <Zap className="h-3.5 w-3.5" aria-hidden="true" />
              Modern electricity utility
            </p>
            <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-white sm:text-5xl">
              VOLTARAS
            </h1>
            <p className="mt-4 max-w-xl text-lg leading-relaxed text-slate-200">
              Smart meters, transparent billing and instant payments for the connected
              home. One platform for your entire electricity journey.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/register"
                className="inline-flex h-12 items-center justify-center gap-2 rounded-md bg-volt-600 px-6 text-base font-medium text-white transition-colors hover:bg-volt-500"
              >
                Get started
                <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </Link>
              <Link
                to="/login"
                className="inline-flex h-12 items-center justify-center rounded-md border border-white/20 bg-white/5 px-6 text-base font-medium text-white transition-colors hover:bg-white/10"
              >
                Sign in
              </Link>
            </div>
            <p className="mt-6 text-xs text-slate-300">
              Internal demonstration platform — no real electricity services are provided.
            </p>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-20">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold text-navy-900 sm:text-3xl">
            Everything your electricity account needs
          </h2>
          <p className="mt-3 text-slate-500">
            Built for consumers and operators alike, with verified data at every step.
          </p>
        </div>
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((feature) => (
            <div
              key={feature.title}
              className="rounded-card border border-slate-200 bg-white p-6 shadow-card"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50">
                <feature.icon className="h-5 w-5 text-volt-600" aria-hidden="true" />
              </div>
              <h3 className="mt-4 text-base font-semibold text-navy-900">{feature.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-500">
                {feature.description}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section className="bg-white">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-20">
          <h2 className="text-center text-2xl font-bold text-navy-900 sm:text-3xl">
            How it works
          </h2>
          <div className="mt-10 grid gap-8 md:grid-cols-3">
            {steps.map((step) => (
              <div key={step.step} className="text-center md:text-left">
                <p className="text-sm font-bold tracking-wide text-volt-600">{step.step}</p>
                <h3 className="mt-2 text-lg font-semibold text-navy-900">{step.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-500">
                  {step.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="bg-navy-900">
        <div className="mx-auto max-w-6xl px-4 py-16 text-center sm:px-6">
          <h2 className="text-2xl font-bold text-white sm:text-3xl">
            Ready to switch to smarter power?
          </h2>
          <p className="mx-auto mt-3 max-w-xl text-slate-300">
            Create a demo account and explore the platform with verified sample data.
          </p>
          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link
              to="/register"
              className="inline-flex h-12 items-center justify-center gap-2 rounded-md bg-volt-600 px-6 text-base font-medium text-white transition-colors hover:bg-volt-500"
            >
              Create account
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </Link>
            <Link
              to="/login"
              className="inline-flex h-12 items-center justify-center rounded-md border border-white/20 bg-white/5 px-6 text-base font-medium text-white transition-colors hover:bg-white/10"
            >
              Sign in
            </Link>
          </div>
        </div>
      </section>
    </>
  )
}
