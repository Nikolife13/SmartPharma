/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        background: '#F4F7F6',
        surface: '#FFFFFF',
        primary: {
          DEFAULT: '#00796B',
          dark: '#004D40',
        },
        muted: '#757575',
        ink: '#1A1A1A',
        border: '#E0E0E0',
        success: '#2E7D32',
        danger: '#D32F2F',
        accent: '#FFB300',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        card: '12px',
        control: '8px',
      },
      boxShadow: {
        soft: '0 2px 8px rgba(0, 0, 0, 0.06)',
        card: '0 4px 16px rgba(0, 0, 0, 0.06)',
      },
      keyframes: {
        shimmer: {
          '100%': { transform: 'translateX(100%)' },
        },
        fadeIn: {
          from: { opacity: 0 },
          to: { opacity: 1 },
        },
      },
      animation: {
        shimmer: 'shimmer 1.5s infinite',
        fadeIn: 'fadeIn 200ms ease-in-out',
      },
    },
  },
  plugins: [],
};
