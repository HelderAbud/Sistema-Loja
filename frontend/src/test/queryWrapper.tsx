import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement, ReactNode } from "react";

export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

export function withQueryClient(ui: ReactElement, client?: QueryClient): ReactElement {
  const qc = client ?? createTestQueryClient();
  return <QueryClientProvider client={qc}>{ui}</QueryClientProvider>;
}

export function TestQueryProvider({
  children,
  client,
}: {
  children: ReactNode;
  client?: QueryClient;
}): ReactElement {
  return (
    <QueryClientProvider client={client ?? createTestQueryClient()}>{children}</QueryClientProvider>
  );
}
