import { Navigate, Outlet } from "react-router-dom";

type Props = {
  authed: boolean;
};

export function ProtectedLayout({ authed }: Props) {
  if (!authed) {
    return <Navigate to="/login" replace />;
  }
  return (
    <div className="app-backdrop">
      <Outlet />
    </div>
  );
}
