import React from "react";
import { createRoot } from "react-dom/client";
import Dashboard from "./dashboard/index";

function renderDashboard() {
  console.log("Rendering Dashboard...");
  const target = document.querySelector('.ch-dashboard');
  if (target) {
    const reactContainer = document.createElement('div');
    target.appendChild(reactContainer);
    
    const root = createRoot(reactContainer);

    root.render(<Dashboard />);
  } else {
    setTimeout(renderDashboard, 100);
  }
}

// Wait for DOM to be ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', renderDashboard);
} else {
  renderDashboard();
}
