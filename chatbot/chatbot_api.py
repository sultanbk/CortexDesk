from flask import Flask, request, jsonify
from flask_cors import CORS
from admin_agent import user, manager

app = Flask(__name__)
CORS(app, origins=["http://localhost:5173"])

@app.route('/chat', methods=['POST'])
def chat():
    print("Received request")
    data = request.get_json()
    print("Data:", data)
    user_message = data.get('message', '')
    if not user_message:
        print("No message provided")
        return jsonify({'error': 'No message provided'}), 400

    responses = []
    original_receive = user.receive

    def receive_and_capture(*args, **kwargs):
        print("In receive_and_capture")
        if len(args) >= 2:
            message = args[0]
            if isinstance(message, dict):
                content = message.get("content", "")
                if content:
                    responses.append(content)
        return original_receive(*args, **kwargs)

    user.receive = receive_and_capture
    print("Calling initiate_chat")
    user.initiate_chat(recipient=manager, message=user_message)
    user.receive = original_receive

    print("Responses:", responses)
    if responses:
        # Join all responses with line breaks for clarity
        return jsonify({'response': "\n\n".join(str(r) for r in responses if r)})
    else:
        return jsonify({'error': 'No response from chatbot'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5050, debug=True)