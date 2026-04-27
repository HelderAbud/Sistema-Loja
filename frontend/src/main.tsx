import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "sonner";
import App from "./App";
import "./theme/tokens.css";
import "./App.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
  },
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        richColors
        closeButton
        position="bottom-right"
        expand={false}
        gap={10}
        toastOptions={{
          duration: 4400,
          classNames: {
            toast: "loja-toast",
            title: "loja-toast-title",
            description: "loja-toast-desc",
          },
        }}
      />
    </QueryClientProvider>
  </StrictMode>,
);
