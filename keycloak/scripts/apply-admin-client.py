#!/usr/bin/env python3
"""Applies the cinx-admin client, its step-up flow and the ACR map from cinx-realm.json to a running realm.

`--import-realm` skips an existing realm and partial import carries no flows, so this replays them with kcadm inside
the Keycloak container. It skips what already exists, so it can run again.

    python3 keycloak/scripts/apply-admin-client.py [--container keycloak] [--realm cinx]
"""
import argparse
import json
import pathlib
import subprocess

REALM_FILE = pathlib.Path(__file__).resolve().parent.parent / "realm" / "cinx-realm.json"
FLOW = "admin-step-up"
CLIENT = "cinx-admin"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--container", default="keycloak")
    parser.add_argument("--realm", default="cinx")
    args = parser.parse_args()
    realm = json.loads(REALM_FILE.read_text())
    r = ["-r", args.realm]

    def kc(*cmd, body=None):
        out = subprocess.run(["docker", "exec", "-i", args.container, "/opt/keycloak/bin/kcadm.sh", *cmd],
                             input=body, capture_output=True, text=True)
        if out.returncode != 0:
            raise SystemExit(f"kcadm {' '.join(cmd[:3])} failed: {out.stderr.strip()}")
        return json.loads(out.stdout) if out.stdout.strip().startswith(("{", "[")) else out.stdout

    # the admin's credentials never leave the container: kcadm reads them from its environment
    subprocess.run(["docker", "exec", args.container, "sh", "-c",
                    "/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master "
                    '--user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" >/dev/null'], check=True)

    flows = {f["alias"]: f for f in realm["authenticationFlows"]}
    configs = {c["alias"]: c for c in realm["authenticatorConfig"]}
    flow_ids = {f["alias"]: f["id"] for f in kc("get", "authentication/flows", *r, "--fields", "alias,id")}

    if FLOW in flow_ids:
        print(f"flow {FLOW}: exists")
    else:
        top = flows[FLOW]
        kc("create", "authentication/flows", *r, "-b",
           json.dumps({k: top[k] for k in ("alias", "description", "providerId", "topLevel", "builtIn")}))

        def build(alias):
            for ex in flows[alias]["authenticationExecutions"]:
                if ex["authenticatorFlow"]:
                    sub = flows[ex["flowAlias"]]
                    kc("create", f"authentication/flows/{alias}/executions/flow", *r, "-b",
                       json.dumps({"alias": sub["alias"], "description": sub["description"], "type": "basic-flow"}))
                    build(sub["alias"])
                else:
                    kc("create", f"authentication/flows/{alias}/executions/execution", *r, "-b", json.dumps({"provider": ex["authenticator"]}))

        build(FLOW)
        flow_ids = {f["alias"]: f["id"] for f in kc("get", "authentication/flows", *r, "--fields", "alias,id")}
        print(f"flow {FLOW}: created")

    # executions start DISABLED: set each requirement (sending its priority back) and attach a missing config;
    # the list comes depth-first with a level, so the parent of a level-n row is the last flow seen at level n-1
    wanted = {(alias, ex.get("flowAlias") or ex["authenticator"]): ex
              for alias, flow in flows.items() for ex in flow["authenticationExecutions"]}
    parents = {0: FLOW}
    for info in kc("get", f"authentication/flows/{FLOW}/executions", *r):
        is_flow = info.get("authenticationFlow", False)
        key = info["displayName"] if is_flow else info["providerId"]
        ex = wanted[(parents[info["level"]], key)]
        if is_flow:
            parents[info["level"] + 1] = key
        if info.get("requirement") != ex["requirement"]:
            kc("update", f"authentication/flows/{FLOW}/executions", *r, "-b",
               json.dumps({"id": info["id"], "requirement": ex["requirement"], "priority": info.get("priority", 0)}))
        if ex.get("authenticatorConfig") and not info.get("authenticationConfig"):
            kc("create", f"authentication/executions/{info['id']}/config", *r, "-b", json.dumps(configs[ex["authenticatorConfig"]]))
    print(f"flow {FLOW}: requirements and configs set")

    # the ACR map first: the client's minimum.acr.value must name a level it maps. A realm update replaces the
    # attribute map, so merge into what is there and read it back
    # read the whole realm: `--fields attributes` drops the nested map
    before = kc("get", f"realms/{args.realm}").get("attributes", {})
    kc("update", f"realms/{args.realm}", "-f", "-", body=json.dumps({"attributes": {**before, **realm["attributes"]}}))
    after = kc("get", f"realms/{args.realm}").get("attributes", {})
    lost = sorted(set(before) - set(after))
    print(f"acr.loa.map: {after.get('acr.loa.map')}" + (f"  (attributes lost: {lost})" if lost else ""))

    if kc("get", "clients", *r, "-q", f"clientId={CLIENT}", "--fields", "id"):
        print(f"client {CLIENT}: exists, left as it is")
    else:
        client = next(c for c in realm["clients"] if c["clientId"] == CLIENT)
        kc("create", "clients", *r, "-f", "-",
           body=json.dumps({**client, "authenticationFlowBindingOverrides": {"browser": flow_ids[FLOW]}}))
        print(f"client {CLIENT}: created")


if __name__ == "__main__":
    main()
