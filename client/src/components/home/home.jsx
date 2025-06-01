import React, { useState, useEffect } from "react";
import "./home.css";
import Sidebar from "./Sidebar";
import ChatPage from "../chat-page/Chatpage";
import RecentChats from "./RecentChats";
import logo from "../../assets/icons/logo.png";

export default function Home() {
  const username = localStorage.getItem("username");
  const [selectedChat, setSelectedChat] = useState(null);


  const handleSelectChat = (chat) => {
    setSelectedChat(chat); 
  };

  useEffect(() => {
    if (!username) return;
  }, [username]);

  const getChatKey = (chat) => {
    if (!chat) return 'no-chat';
    if (typeof chat === 'string') return chat; 
    if (chat.isGroup) return `group-${chat.id}`; 
    if (chat.id) return `message-${chat.id}`; 
    if (chat.receiver) return chat.receiver; 
    if (chat.sender) return chat.sender; 
    return JSON.stringify(chat); 
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
              chat={selectedChat} 
              key={getChatKey(selectedChat)} 
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
