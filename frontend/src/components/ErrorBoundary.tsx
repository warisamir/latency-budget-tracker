import React, { ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export default class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-slate-950 p-6 flex items-center justify-center">
          <div className="max-w-md bg-slate-900 border-l-4 border-red-600 rounded-lg p-6">
            <h2 className="text-xl font-bold text-red-400 mb-2">Rendering Error</h2>
            <p className="text-slate-400 text-sm mb-4">Something went wrong while rendering the page.</p>
            <pre className="bg-slate-800 text-red-300 text-xs p-3 rounded overflow-auto max-h-40">
              {this.state.error?.message || 'Unknown error'}
            </pre>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded"
            >
              Reload Page
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
