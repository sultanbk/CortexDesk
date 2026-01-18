from autogen import GroupChat, GroupChatManager, UserProxyAgent
from agents.classifier_agent import get_classifier_agent
from agents.knoweledge_agent import get_knowledge_base_agent
from utility.llm_config import llm_config

# Termination condition
def is_termination_msg(message):
    return isinstance(message, dict) and message.get("content", "").strip().upper() == "TERMINATE"


# Create agents
classifier = get_classifier_agent()
kb_agent = get_knowledge_base_agent()

# Create user agent
user = UserProxyAgent(
    name="User",
    human_input_mode="TERMINATE",
    code_execution_config=False,
    is_termination_msg=is_termination_msg,
)

# Create group chat with all agents
groupchat = GroupChat(
    agents=[user, classifier, kb_agent],  # , notifier],
    messages=[],
    speaker_selection_method="Auto",
    allow_repeat_speaker=False,
    max_round=6
)

# Create group chat manager
manager = GroupChatManager(
    groupchat=groupchat,
    llm_config=llm_config,
    is_termination_msg=is_termination_msg
)


if __name__=="__main__":
    # Trigger conversation
    user.initiate_chat(
        recipient=manager,
        message="Hi"
    )