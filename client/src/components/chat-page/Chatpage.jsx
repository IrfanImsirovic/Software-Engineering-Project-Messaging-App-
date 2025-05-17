import React, { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import "./Chatpage.css";
import sendIcon from "../../assets/icons/send_icon.png";
import userIcon from "../../assets/icons/user.png";

const API_URL = import.meta.env.VITE_API_URL;
const SOCKET_URL = import.meta.env.VITE_API_URL.replace(/^http/, "ws") + "/ws";

export default function ChatPage({ username, chat }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);
  const chatBoxRef = useRef(null);

  const isGroup = chat?.isGroup === true;
  const chatTitle = isGroup ? chat.name : (chat.username || chat);

  // Auto-scroll
  useEffect(() => {
    chatBoxRef.current?.scrollTo(0, chatBoxRef.current.scrollHeight);
  }, [messages]);

  // Load history
  useEffect(() => {
    const token = localStorage.getItem("authToken");
    const url = isGroup
      ? `${API_URL}/api/group-messages/history?groupId=${chat.id}`
      : `${API_URL}/api/messages/history?friendUsername=${chat.username || chat}`;

    fetch(url, {
      headers: { Authorization: "Bearer " + token },
    })
      .then((res) => res.json())
      .then(setMessages)
      .catch(console.error);
  }, [chat, username]);

  // WebSocket connection
  useEffect(() => {
    const token = localStorage.getItem("authToken");
    const client = new Client({
      brokerURL: SOCKET_URL,
      connectHeaders: { Authorization: "Bearer " + token.trim() },
      reconnectDelay: 5000,
      debug: (str) => console.log("STOMP DEBUG:", str),
      onConnect: () => {
        console.log("✅ Connected to WebSocket");
        setConnected(true);
        if (isGroup) {
          const topic = `/topic/group/${chat.id}`;
          console.log("📡 Subscribing to group topic:", topic);
          client.subscribe(topic, (message) => {
            const receivedMsg = JSON.parse(message.body);
            console.log("📨 Received group message:", receivedMsg);
            
            // Map the received message to the format expected by the UI
            const formattedMsg = {
              groupId: chat.id,
              sender: receivedMsg.sender,
              content: receivedMsg.content,
              timestamp: receivedMsg.timestamp,
            };
            
            setMessages((prev) => [...prev, formattedMsg]);
          });
        } else {
          const topic = `/topic/messages/${username}`;
          console.log("📡 Subscribing to topic:", topic);

          client.subscribe(topic, (message) => {
            const msg = JSON.parse(message.body);
            console.log("📨 Received message:", msg);
            
            setMessages((prev) => [...prev, msg]);
          });
        }
      },
      onStompError: (frame) => {
        console.error("❌ Broker error:", frame.headers["message"]);
      },
      onDisconnect: () => {
        console.log("🔌 Disconnected");
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [username, chat]);

  // Send message
  const sendMessage = () => {
    const client = clientRef.current;
    if (!input.trim() || !connected || !client?.connected) return;

    const now = new Date().toISOString();

    const messagePayload = isGroup
      ? {
          groupId: chat.id,
          sender: username,
          content: input.trim(),
          timestamp: now,
        }
      : {
          sender: username,
          receiver: chat.username || chat,
          content: input.trim(),
          timestamp: now,
        };

    client.publish({
      destination: isGroup ? "/app/group" : "/app/chat",
      body: JSON.stringify(messagePayload),
    });

    // Don't add message locally - let the WebSocket handle it
    // The message will be received back through the subscription
    setInput("");
  };

  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  const isFirstInSequence = (index) =>
    index === 0 || messages[index].sender !== messages[index - 1].sender;

  return (
    <div className="chat-container">
      <div className="chat-header">
        <div className="friend-profile">
          <div className="friend-avatar">
            <img src={userIcon} alt="User" />
          </div>
          <span className="friend-name">{chatTitle}</span>
        </div>
        {!connected && <p className="connection-status">🔌 Connecting...</p>}
      </div>

      <div className="chat-box" ref={chatBoxRef}>
        {messages.map((msg, idx) => {
          console.log("🧾 Rendering message:", msg)
          const isMe = msg.sender === username;
          const isSystem = msg.sender === "SYSTEM";
          const first = isFirstInSequence(idx);

          // Handle system messages differently
          if (isSystem) {
            return (
              <div key={idx} className="system-wrapper">
                <div className="message-bubble system">
                  {msg.content}
                </div>
              </div>
            );
          }

          return (
            <div
              key={idx}
              className={`message-wrapper ${isMe ? "sent-wrapper" : "received-wrapper"}`}
            >
              {!isMe && first && (
                <div className="sender-avatar">
                  <img src={userIcon} alt="sender" />
                </div>
              )}
              {!isMe && !first && <div className="avatar-spacer"></div>}

              <div className={`message-container ${isMe ? "sent-container" : "received-container"}`}>
                <div className={`message-bubble ${isMe ? "sent" : "received"}`}>
                  {!isMe && first && <div className="message-sender">{msg.sender}</div>}
                  <div className="message-content-wrapper">
                    <div className="message-content">{msg.content}</div>
                    <span className="message-time">{formatTime(msg.timestamp)}</span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="chat-footer">
        <div className="chat-input">
          <input
            type="text"
            placeholder="Type a message..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendMessage()}
            disabled={!connected}
          />
          <button onClick={sendMessage} disabled={!connected || !input.trim()}>
            <img src={sendIcon} alt="Send" />
          </button>
        </div>
      </div>
    </div>
  );
}
