import React from "react";
import "./Navbar.css";
import { Link } from "react-router-dom";

export default function Navbar() {
    return (
      <nav className="navbar">
        <div className="navbar-left">
          <span className="navbar-brand"> Link </span>
        </div>
  
       
        <div className="navbar-center">
        <Link to="/home" className="navbar-link">Home</Link>
        <Link to="/profile" className="navbar-link">Profile</Link>
      </div>
        
  
        <div className="navbar-right">
          <img
            src="https://img.freepik.com/premium-vector/avatar-profile-icon-flat-style-male-user-profile-vector-illustration-isolated-background-man-profile-sign-business-concept_157943-38764.jpg?semt=ais_hybrid"
            alt="Avatar"
            className="navbar-avatar"
          />
          <span className="navbar-username">Ahmed</span>
          <button className="logout-btn">Logout</button>
        </div>
      </nav>
    );
  }
  