import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        navy: {
          50: '#f0f4fa',
          100: '#dce6f3',
          200: '#b9cce8',
          300: '#8dacd9',
          400: '#5b84c4',
          500: '#3560a8',
          600: '#254a8c',
          700: '#1d3a70',
          800: '#162c57',
          900: '#0f2142',
          950: '#081530',
        },
        volt: {
          50: '#eef4ff',
          100: '#dbe7fe',
          200: '#bcd2fd',
          300: '#8fb4fb',
          400: '#5a8ef8',
          500: '#2f6bf0',
          600: '#1f50e0',
          700: '#1a3fc0',
          800: '#1a379b',
          900: '#1a327a',
        },
      },
      borderRadius: {
        // Cap decorative radius at 8px per the design direction.
        card: '8px',
      },
      boxShadow: {
        card: '0 1px 2px rgba(8, 21, 48, 0.06), 0 4px 16px rgba(8, 21, 48, 0.08)',
      },
      fontFamily: {
        sans: [
          'ui-sans-serif',
          'system-ui',
          '-apple-system',
          '"Segoe UI"',
          'Roboto',
          '"Helvetica Neue"',
          'Arial',
          'sans-serif',
        ],
      },
    },
  },
  plugins: [],
} satisfies Config
