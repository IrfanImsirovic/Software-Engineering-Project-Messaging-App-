import React, { useState, useEffect } from "react";
import "./home.css";
import Sidebar from "./Sidebar";
import ChatPage from "../chat-page/Chatpage";
import RecentChats from "./RecentChats";
import logo from "../../assets/icons/logo.png";

export default function Home() {
  const username = localStorage.getItem("username");
  const [selectedChat, setSelectedChat] = useState(null); // Can hold either a friend or group
  console.log("🏠 Home render - selectedChat:", selectedChat);
  console.log("🏠 Home render - username:", username);


  // Handle selecting either a group or individual chat
  const handleSelectChat = (chat) => {
    setSelectedChat(chat); // This can be a friend or a group
  };

  // Make sure selectedChat is updated properly if a new friend or group is selected
  useEffect(() => {
    if (!username) return;
    // Fetch recent chats and groups on initial load (this might already be handled in RecentChats)
  }, [username]);

  // Generate a unique key for the ChatPage component
  const getChatKey = (chat) => {
    if (!chat) return 'no-chat';
    if (typeof chat === 'string') return chat; // Just a username
    if (chat.isGroup) return `group-${chat.id}`; // Group chat
    if (chat.id) return `message-${chat.id}`; // Message object
    if (chat.receiver) return chat.receiver; // Message DTO
    if (chat.sender) return chat.sender; // Another message format
    return JSON.stringify(chat); // Fallback
  };

  return (
    <div className="home-page">
      <RecentChats username={username} onSelectFriend={handleSelectChat} />

      <div className="main-content">
        <Sidebar username={username} onSelectFriend={handleSelectChat} />

        {selectedChat ? (
          <div style={{ flex: 1 }}>
            <ChatPage
              username={username}
              chat={selectedChat} // Pass the entire chat object
              key={getChatKey(selectedChat)} // Use the helper function to generate a unique key
            />
          </div>
        ) : (
          <div className="welcome-screen">
            <img src={logo} alt="App Logo" className="welcome-logo" />
            <h2 className="welcome-message">Connect With Your Friends!</h2>
          </div>
        )}
      </div>
    </div>
  );
}
