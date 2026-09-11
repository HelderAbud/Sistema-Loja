import { Link } from "react-router-dom";
import { BRAND_NAME } from "../../brand";

export function StoreHeader() {
  return (
    <header className="store-topbar">
      <div className="store-shell store-topbar-content">
        <Link to="/" className="store-logo">
          <span aria-hidden>L</span>
          <strong>{BRAND_NAME}</strong>
        </Link>
        <nav className="store-nav">
          <Link to="/">Home</Link>
          <Link to="/pitch">Pitch</Link>
          <Link to="/login">Entrar</Link>
        </nav>
      </div>
    </header>
  );
}
