import React, { useState, useRef, useEffect } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRobot, faThumbsUp, faThumbsDown, faPaperPlane } from "@fortawesome/free-solid-svg-icons";
import { getCurrentUser } from "../auth/auth";

const CHATBOT_API_URL = "http://localhost:5050/chat";

export default function ChatLauncher() {
  const currentUser = getCurrentUser();
  const customerId = currentUser?.userId || 0;
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [displayedText, setDisplayedText] = useState("");
  const messagesEndRef = useRef(null);

  // Typewriter effect for welcome message
  useEffect(() => {
    if (open && messages.length === 0) {
      const fullText = "How can I help you?";
      let currentIndex = 0;
      setDisplayedText("");
      
      const interval = setInterval(() => {
        if (currentIndex <= fullText.length) {
          setDisplayedText(fullText.slice(0, currentIndex));
          currentIndex++;
        } else {
          clearInterval(interval);
        }
      }, 50);
      
      return () => clearInterval(interval);
    }
  }, [open, messages.length]);

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, isTyping]);

  // Mark as read when opening chat
  useEffect(() => {
    if (open) {
      setUnreadCount(0);
    }
  }, [open]);

  // Increment unread count when bot sends message while closed
  const addBotMessage = (text) => {
    setMessages((msgs) => [...msgs, { sender: "bot", text }]);
    if (!open) {
      setUnreadCount((count) => count + 1);
    }
  };

  // Feedback state
  const [showFeedback, setShowFeedback] = useState(false);
  const [lastAIResponse, setLastAIResponse] = useState("");
  const [lastUserInput, setLastUserInput] = useState("");
  const [feedbackMsg, setFeedbackMsg] = useState("");

  const sendMessage = async () => {
    if (!input.trim()) return;
    
    const userMsg = { sender: "user", text: input };
    setMessages((msgs) => [...msgs, userMsg]);
    setLastUserInput(input);
    setInput("");
    setLoading(true);
    setIsTyping(true);
    setShowFeedback(false);
    setFeedbackMsg("");
    
    try {
      const res = await fetch(CHATBOT_API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: input }),
      });
      const data = await res.json();
      setIsTyping(false);
      
      if (data.response) {
        addBotMessage(data.response);
        setLastAIResponse(data.response);
        setShowFeedback(true);
      } else {
        addBotMessage("Sorry, I couldn't get a response. Please try again.");
      }
    } catch (e) {
      setIsTyping(false);
      addBotMessage("Error connecting to chatbot. Please check your connection.");
      console.error("Chat error:", e);
    }
    setLoading(false);
  };

  // Feedback handlers
  const handleFeedback = async (resolved) => {
    setShowFeedback(false);
    if (resolved) {
      setFeedbackMsg("🎉 Great! We're glad your issue is resolved. Thank you!");
    } else {
      // Try to extract category and resolution from lastAIResponse
      let category = "";
      let issueCategoryId = 12; // Default to "Billing / Account" (fallback)
      // Map category names from prompt.py to IDs
      const categoryMap = {
        "No Internet": 1,
        "Slow Internet Speed": 2,
        "Router / ONT Issue": 3,
        "Wi-Fi Configuration Issue": 4,
        "Network Outage": 5,
        "Slow Performance": 6,
        "Authentication Issue": 7,
        "Hardware Failure": 8,
        "Application Bug": 9,
        "Change Request": 10,
        "Access Request": 11,
        "Billing / Account": 12,
        "Other": 12  // Map "Other" to Billing/Account (category 12)
      };
      try {
        // Try to extract JSON from the response (it might be mixed with other text)
        const jsonMatch = lastAIResponse.match(/\{[\s\S]*?"category"[\s\S]*?\}/);
        if (jsonMatch) {
          const parsed = JSON.parse(jsonMatch[0]);
          category = parsed.category || "";
          console.log("Extracted category:", category);
        }
        // Map category string to ID using exact match
        if (category && categoryMap[category]) {
          issueCategoryId = categoryMap[category];
          console.log("Mapped to issueCategoryId:", issueCategoryId);
        }
      } catch (e) {
        console.log("Error parsing category:", e);
      }
      // Send ticket to backend with required fields
      const payload = {
        customerId,
        issueCategoryId,
        description: lastUserInput
      };
      console.log("Sending ticket payload:", payload);
      try {
        await fetch("http://localhost:9091/api/tickets", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(payload)
});
        setFeedbackMsg("⚠️ We're escalating this issue to IT support. A ticket has been created.");
      } catch {
        setFeedbackMsg("❌ Failed to create ticket. Please try again later.");
      }
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <>
      <button
        className="chat-float-btn"
        onClick={() => setOpen((o) => !o)}
        aria-label={open ? "Close chat" : "Open chat"}
        title="Chat with AI Support"
      >
        <FontAwesomeIcon icon={faRobot} className="chat-btn-icon" />
        {unreadCount > 0 && !open && (
          <span className="chat-unread-badge">{unreadCount > 9 ? "9+" : unreadCount}</span>
        )}
      </button>

      {open && (
        <div className="chat-window" role="dialog" aria-modal="true" aria-label="Chat window">
          <div className="chat-header">
            <div className="chat-header-left">
              <div className="chat-header-text">
                <span className="chat-header-title">CortexDesk</span>
                <span className="chat-header-status">Always here to help</span>
              </div>
            </div>
            <button 
              className="chat-close-btn" 
              onClick={() => setOpen(false)} 
              aria-label="Close chat"
              title="Close chat"
            >
              ✕
            </button>
          </div>

          <div className="chat-messages">
            {messages.length === 0 && (
              <div className="chat-welcome">
                <div className="chat-welcome-icon">
                  <FontAwesomeIcon icon={faRobot} style={{ fontSize: '2em', color: '#0066cc' }} />
                </div>
                <h3 className="chat-typewriter">{displayedText}<span className="chat-cursor">|</span></h3>
                <p>Ask about network issues, tickets, or anything else!</p>
              </div>
            )}
            
            {messages.map((msg, i) => (
              <div key={`msg-${msg.sender}-${msg.text.substring(0, 20)}-${i}`} className={`chat-msg-wrapper ${msg.sender}`}>
                <div className={`chat-msg ${msg.sender}`}>
                  {msg.text}
                </div>
              </div>
            ))}

            {isTyping && (
              <div className="chat-msg-wrapper bot">
                <div className="chat-msg bot typing">
                  <span className="typing-dot"></span>
                  <span className="typing-dot"></span>
                  <span className="typing-dot"></span>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {feedbackMsg && (
            <div className="chat-feedback-msg">
              {feedbackMsg}
            </div>
          )}

          {showFeedback && (
            <div className="chat-feedback-buttons">
              <button
                className="chat-feedback-btn positive"
                onClick={() => handleFeedback(true)}
              >
                <FontAwesomeIcon icon={faThumbsUp} /> Helpful
              </button>
              <button
                className="chat-feedback-btn negative"
                onClick={() => handleFeedback(false)}
              >
                <FontAwesomeIcon icon={faThumbsDown} /> Not helpful
              </button>
            </div>
          )}

          <form
            className="chat-input-row"
            onSubmit={e => { e.preventDefault(); sendMessage(); }}
          >
            <textarea
              className="chat-input"
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Type your question..."
              rows={1}
              disabled={loading}
              aria-label="Message input"
            />
            <button
              className="chat-send-btn"
              type="submit"
              disabled={loading || !input.trim()}
              aria-label="Send message"
              title="Send (Enter)"
            >
              {loading ? "⏳" : <FontAwesomeIcon icon={faPaperPlane} />}
            </button>
          </form>
        </div>
      )}
    </>
  );
}
