import React from "react";
import { useNavigate } from "react-router-dom";
import "./home.css";

export default function Home() {
  const navigate = useNavigate();

  const handleLogout = () => {
    console.log("Logging out...");
    navigate("/login"); // Redirect back to login on logout
  };

  return (
    <div className="home-page">
      <div className="content">
        <h1>Welcome to Your Dashboard</h1>
        <p>You have successfully logged in!</p>
        <button onClick={handleLogout} className="logout-button">Logout</button>
      </div>
    </div>
  );
}
