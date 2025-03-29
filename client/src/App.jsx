import { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Register from "./components/register/register";
import Login from "./components/login/login";
import Home from "./components/home/home"; // New home component

import "./App.css";
import ChatScreen from "./components/chatUI/chatScreen";
import Profile from "./components/profile/Profile";

function App() {
  const [message, setMessage] = useState("Loading...");

  useEffect(() => {
    fetch(import.meta.env.VITE_API_URL + "/api/test", {
      method: "GET",
      credentials: "include",
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("Unauthorized");
        }
        return response.text();
      })
      .then((data) => setMessage(data))
      .catch((error) => setMessage("Error fetching data: " + error.message));
  }, []);

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Navigate to="/register" replace />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route path="/home" element={<Home />} />
        <Route path="/chat" element={<div><ChatScreen/></div>} />
        <Route path="/profile" element={<div><Profile/></div>} />
      </Routes>
    </Router>
  );
}

export default App;
