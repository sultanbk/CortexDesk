from autogen import AssistantAgent
from tools.knoweledge_base_tool import search_similar_solution
from utility.llm_config import llm_config


def get_knowledge_base_agent():

    knowledge_agent = AssistantAgent(
        name="KnowledgeBaseAgent",
        system_message=(
            """You are a specialized Network & IT Support Assistant. Your primary goal is to resolve technical issues by finding similar past resolutions.
Protocol:
Identify: When a user describes a technical problem, extract the core issue.
Retrieve: Use the search_similar_solution tool with a concise search query based on the user's description.
Respond: Summarize the most relevant solution clearly. If no relevant solution is found, advise the user to contact a senior engineer.
Closure: Once the solution is provided, end your response with 'TERMINATE'.
Constraint: If the user’s input is not a technical issue (e.g., greetings or general talk), respond politely but do not call the tool."""
        ),
        llm_config=llm_config,
        code_execution_config={"use_docker": False},
    )

    # Register tool with LLM and executor
    # 1. LLm knows when to call the tool
    knowledge_agent.register_for_llm(
        name="search_similar_solution",
        description="Finds technical fixes for network issues (e.g., ONT, Wi-Fi, VLAN, No Internet) by searching a database of past tickets. Input 'query' should be the core technical problem.",
    )(search_similar_solution)

    # 2. Executed the tool
    knowledge_agent.register_for_execution(name="search_similar_solution")(
        search_similar_solution
    )

    return knowledge_agent
