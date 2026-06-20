/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        cream: '#FAF7F0',
        'accent-pink': '#F8C8DC',
        'accent-butter': '#FFF1A8',
        'accent-lavender': '#C8B6E2',
        'accent-mint': '#B8E0D2',
        ink: '#2E2A33',
        'ink-muted': '#6B6478',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: '0 2px 8px rgba(46, 42, 51, 0.08)',
        'card-hover': '0 4px 16px rgba(46, 42, 51, 0.12)',
      },
    },
  },
  plugins: [],
};

