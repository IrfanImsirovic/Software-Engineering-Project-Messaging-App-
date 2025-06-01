import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./Sidebar.css";
import userIcon from "../../assets/icons/user.png";
import groupIcon from "../../assets/icons/group.png";
import notificationIcon from "../../assets/icons/notification.png";
import addFriendIcon from "../../assets/icons/add-friend.png";
import acceptIcon from "../../assets/icons/accept_icon.png";
import rejectIcon from "../../assets/icons/reject_icon.png";
import logoutIcon from "../../assets/icons/logout.png";
import UserProfile from "./UserProfile";
import ProfilePicture from "../common/ProfilePicture";
import "../common/ProfilePicture.css";

const API_URL = import.meta.env.VITE_API_URL;

export default function Sidebar({ username, onSelectFriend }) {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("friends");
  const [friends, setFriends] = useState([]);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [newFriend, setNewFriend] = useState("");
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [showProfile, setShowProfile] = useState(false);

  const authHeader = {
    "Authorization": "Bearer " + localStorage.getItem("authToken")
  };

  useEffect(() => {
    if (!username) return;
    fetch(`${API_URL}/api/friends/list?username=${username}`, {
      headers: authHeader
    })
      .then(res => res.json())
      .then(data => setFriends(data))
      .catch(() => setFriends([]));
  }, [username]);

  useEffect(() => {
    if (activeTab !== "requests" || !username) return;
    fetch(`${API_URL}/api/friends/pending?username=${username}`, {
      headers: authHeader
    })
      .then(res => res.json())
      .then(data => setPendingRequests(data))
      .catch(() => setPendingRequests([]));
  }, [activeTab, username]);

  useEffect(() => {
    let timeout;
    if (message || errorMessage) {
      timeout = setTimeout(() => {
        setMessage("");
        setErrorMessage("");
      }, 4000);
    }
    return () => clearTimeout(timeout);
  }, [message, errorMessage]);

  const handleAddFriend = async () => {
    setMessage(""); setErrorMessage("");
    if (!newFriend.trim()) return;
    try {
      const res = await fetch(`${API_URL}/api/friends/request?senderUsername=${username}&receiverUsername=${newFriend}`, {
        method: "POST",
        headers: authHeader
      });
      if (!res.ok) throw new Error(await res.text());
      setMessage("Friend request sent!");
      setNewFriend("");
    } catch (e) {
      setErrorMessage(e.message);
    }
  };

  const handleAccept = async (senderUsername) => {
    setMessage(""); setErrorMessage("");
    try {
      const res = await fetch(`${API_URL}/api/friends/accept?receiverUsername=${username}&senderUsername=${senderUsername}`, {
        method: "PUT",
        headers: authHeader
      });
      if (!res.ok) throw new Error(await res.text());
      setPendingRequests(pendingRequests.filter(r => r.sender.username !== senderUsername));
      setMessage("Friend request accepted!");
    } catch (e) {
      setErrorMessage(e.message);
    }
  };

  const handleReject = async (senderUsername) => {
    setMessage(""); setErrorMessage("");
    try {
      const res = await fetch(`${API_URL}/api/friends/reject?receiverUsername=${username}&senderUsername=${senderUsername}`, {
        method: "PUT",
        headers: authHeader
      });
      if (!res.ok) throw new Error(await res.text());
      setPendingRequests(pendingRequests.filter(r => r.sender.username !== senderUsername));
      setMessage("Friend request rejected!");
    } catch (e) {
      setErrorMessage(e.message);
    }
  };

  const handleRemoveFriend = async (friendUsername) => {
    setMessage(""); setErrorMessage("");
    try {
      const res = await fetch(`${API_URL}/api/friends/remove?username=${username}&friendUsername=${friendUsername}`, {
        method: "DELETE",
        headers: authHeader
      });
      if (!res.ok) throw new Error(await res.text());
      setFriends(friends.filter(f => f !== friendUsername));
      setMessage("Friend removed.");
    } catch (e) {
      setErrorMessage(e.message);
    }
  };
  
  const handleLogout = () => {
    localStorage.removeItem("authToken");
    localStorage.removeItem("username");
    navigate("/login");
  };

  const handleProfileClick = () => {
    setShowProfile(true);
  };

  const handleCloseProfile = () => {
    setShowProfile(false);
  };

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <button className={activeTab === "friends" ? "active" : ""} onClick={() => setActiveTab("friends")}> 
          <img src={groupIcon} alt="Friends" className="sidebar-tab-icon" />
        </button>
        <button className={activeTab === "requests" ? "active" : ""} onClick={() => setActiveTab("requests")}> 
          <img src={notificationIcon} alt="Pending" className="sidebar-tab-icon" />
        </button>
        <button className={activeTab === "addFriend" ? "active" : ""} onClick={() => setActiveTab("addFriend")}> 
          <img src={addFriendIcon} alt="Add Friend" className="sidebar-tab-icon" />
        </button>
      </div>
      <div className="sidebar-separator" />
      <div className="sidebar-tab-label">
        {activeTab === "friends" && "Friend List"}
        {activeTab === "requests" && "Pending Requests"}
        {activeTab === "addFriend" && "Add a Friend"}
      </div>
      
      <div className="tab-container">
        {activeTab === "friends" && (
          <div className="tab-content">
            <ul className="friends-list">
              {friends.length > 0 ? friends.map(friend => (
                <li 
                  key={friend} 
                  className="friend-item" 
                  onClick={() => onSelectFriend(friend)}
                >
                  <div className="friend-profile">
                    <div className="friend-avatar">
                      <ProfilePicture username={friend} />
                    </div>
                    <div className="friend-name">
                      {friend}
                    </div>
                  </div>
                  <button 
                    className="icon-button" 
                    onClick={(e) => {
                      e.stopPropagation(); 
                      handleRemoveFriend(friend);
                    }} 
                    title="Remove Friend"
                  >
                    <img src={rejectIcon} alt="Remove" className="action-icon" />
                  </button>
                </li>
              )) : <p className="empty-message">No friends yet.</p>}
            </ul>
            {activeTab === "friends" && message && <div className="message success fade-out">{message}</div>}
            {activeTab === "friends" && errorMessage && <div className="message error fade-out">{errorMessage}</div>}
          </div>
        )}
        {activeTab === "requests" && (
          <div className="tab-content">
            <ul className="pending-requests">
              {pendingRequests.length > 0 ? pendingRequests.map(req => (
                <li key={req.id} className="pending-item">
                  <div className="friend-profile">
                    <div className="friend-avatar">
                      <ProfilePicture username={req.sender.username} />
                    </div>
                    <span className="username">{req.sender.username}</span>
                  </div>
                  <div className="action-buttons">
                    <button className="icon-button" onClick={() => handleAccept(req.sender.username)} title="Accept">
                      <img src={acceptIcon} alt="Accept" className="action-icon" />
                    </button>
                    <button className="icon-button" onClick={() => handleReject(req.sender.username)} title="Reject">
                      <img src={rejectIcon} alt="Reject" className="action-icon" />
                    </button>
                  </div>
                </li>
              )) : <p className="empty-message">No pending requests.</p>}
            </ul>
            {activeTab === "requests" && message && <div className="message success fade-out">{message}</div>}
            {activeTab === "requests" && errorMessage && <div className="message error fade-out">{errorMessage}</div>}
          </div>
        )}
        {activeTab === "addFriend" && (
          <div className="tab-content add-friend">
            <div className="add-friend-row">
              <input type="text" placeholder="Enter username" value={newFriend} onChange={e => setNewFriend(e.target.value)} />
              <button className="add-friend-icon-btn" onClick={handleAddFriend} title="Send Friend Request">
                <img src={addFriendIcon} alt="Add Friend" className="add-friend-icon" />
              </button>
            </div>
            {activeTab === "addFriend" && message && <div className="message success fade-out">{message}</div>}
            {activeTab === "addFriend" && errorMessage && <div className="message error fade-out">{errorMessage}</div>}
          </div>
        )}
      </div>
      
      <div className="sidebar-footer">
        <button onClick={handleProfileClick} className="profile-button" title="Profile">
          <img src={userIcon} alt="Profile" />
        </button>
        <button onClick={handleLogout} className="logout-button" title="Logout">
          <img src={logoutIcon} alt="Logout" />
        </button>
      </div>
      
      {showProfile && (
        <div className="profile-modal-overlay" onClick={handleCloseProfile}>
          <div className="profile-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="profile-header">
              <h2>User Profile</h2>
              <button className="close-button" onClick={handleCloseProfile}>&times;</button>
            </div>
            <UserProfile username={username} />
          </div>
        </div>
      )}
    </div>
  );
}
