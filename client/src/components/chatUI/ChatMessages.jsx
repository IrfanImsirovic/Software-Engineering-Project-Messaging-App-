import React from 'react';

export default function ChatMessages() {
  const messages = [
    { text: 'Hi there!', type: 'incoming' },
    { text: 'Hello! How are you?', type: 'outgoing' },
    { text: 'I’m good! How about you?', type: 'incoming' },
  ];

  return (
    <div className="chat-messages">
      {messages.map((msg, index) => (
        <div key={index} className={`message ${msg.type}`}>
          {msg.text}
        </div>
      ))}
    </div>
  );
}
