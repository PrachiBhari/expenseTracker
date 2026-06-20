import { Component, type ReactNode, type ErrorInfo } from 'react';
import { AlertCircle } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  reset = () => this.setState({ hasError: false });

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;

      return (
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-4 p-8 text-center">
          <div className="w-14 h-14 rounded-2xl bg-accent-pink/40 flex items-center justify-center">
            <AlertCircle size={24} className="text-rose-500" />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-ink mb-1">Something went wrong</h2>
            <p className="text-sm text-ink-muted max-w-xs">
              This section failed to load. Try refreshing or click below to retry.
            </p>
          </div>
          <button
            onClick={this.reset}
            className="px-4 py-2 bg-ink text-white text-sm font-medium rounded-xl
              hover:bg-ink/90 transition-colors"
          >
            Try again
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
