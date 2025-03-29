import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../home/Navbar";
import "./Profile.css";

export default function Profile() {
  const { id } = useParams(); // assuming route is /profile/:id
  const [user, setUser] = useState(null);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const res = await fetch(import.meta.env.VITE_API_URL + `/api/users/${id}`);
        const data = await res.json();
        setUser(data);
      } catch (err) {
        console.error("Failed to fetch user", err);
      }
    };

    fetchUser();
  }, [id]);

  if (!user) return <p>Loading...</p>;

  return (
    <>
      <Navbar />
      <div className="profile-container">
        <div className="profile-card">
          <img
            src={`http://localhost:8080${user.profileImagePath || "/images/default.jpg"}`}
            alt="Profile"
            className="profile-avatar"
          />
          <h2 className="profile-name">{user.username}</h2>
          <p className="profile-email">{user.email}</p>

          <button className="logout-btn">Edit profile</button>
          <button className="logout-btn">Delete profile</button>
        </div>
      </div>
    </>
  );
}
