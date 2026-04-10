#!/usr/bin/env python3
"""Read ECS describe-task-definition taskDefinition JSON from stdin; set CONNTO_OTP_DEV_RETURN_CODE=true on first container; write stdout."""
import json
import sys

REMOVE = frozenset(
    {
        "taskDefinitionArn",
        "revision",
        "status",
        "requiresAttributes",
        "compatibilities",
        "registeredAt",
        "registeredBy",
        "deregisteredAt",
        "deregisteredBy",
    }
)

td = json.load(sys.stdin)
for k in REMOVE:
    td.pop(k, None)

cd0 = td["containerDefinitions"][0]
env = cd0.get("environment") or []
cd0["environment"] = [e for e in env if e.get("name") != "CONNTO_OTP_DEV_RETURN_CODE"]
cd0["environment"].append({"name": "CONNTO_OTP_DEV_RETURN_CODE", "value": "true"})

json.dump(td, sys.stdout, separators=(",", ":"))
