import os
from pathlib import Path
from autogen import AssistantAgent

from utility.prompt import classifier_prompt
from utility.llm_config import llm_config

from dotenv import load_dotenv
load_dotenv()

import json
from pydantic import BaseModel, ValidationError


class TicketLabel(BaseModel):
    ticket: str
    category: str


def get_classifier_agent():
    agent = AssistantAgent(
        name="ClassifierAgent",
        llm_config=llm_config,
        system_message=classifier_prompt
    )
    return agent


def validate_classification(raw_text: str) -> TicketLabel:
    """Validate raw model text as JSON matching TicketLabel.

    Raises ValidationError or json.JSONDecodeError on failure.
    """
    body = raw_text.strip()
    if body.startswith("``"):
        # remove code fences
        parts = body.split("```")
        # find the first non-empty JSON-like chunk
        for p in parts:
            p2 = p.strip()
            if p2.startswith("{"):
                body = p2
                break

    obj = json.loads(body)
    return TicketLabel(**obj)


def safe_parse_or_default(raw_text: str) -> TicketLabel:
    """Try to parse and validate; if invalid, return a TicketLabel with category 'Other'."""
    try:
        return validate_classification(raw_text)
    except (ValidationError, json.JSONDecodeError):
        return TicketLabel(ticket=raw_text.strip(), category="Other")