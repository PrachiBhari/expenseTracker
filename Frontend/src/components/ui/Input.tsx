import { forwardRef, type InputHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

// forwardRef lets React Hook Form register this input via a ref (uncontrolled pattern)
const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, className = '', id, ...rest }, ref) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s+/g, '-');

    return (
      <div className="flex flex-col gap-1">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-ink">
            {label}
          </label>
        )}
        <input
          id={inputId}
          ref={ref}
          className={`w-full h-10 px-3 rounded-xl border text-sm text-ink
            placeholder:text-ink-muted/50 transition-colors
            focus:outline-none focus:ring-2 focus:ring-offset-1
            ${
              error
                ? 'border-red-300 bg-red-50 focus:ring-red-200'
                : 'border-gray-200 bg-white hover:border-gray-300 focus:ring-accent-lavender'
            } ${className}`}
          {...rest}
        />
        {error && <p className="text-xs text-red-500">{error}</p>}
        {!error && helperText && <p className="text-xs text-ink-muted">{helperText}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';
export default Input;
