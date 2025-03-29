import ChatHeader from './ChatHeader';
import ChatMessages from './ChatMessages';
import ChatInput from './ChatInput';
import './chatScreen.css';

export default function ChatScreen() {
  return (
    <div className="chat-container">
      <ChatHeader />
      <ChatMessages />
      <ChatInput />
    </div>
  );
}
