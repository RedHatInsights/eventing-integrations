import sys
import json
import base64
import helpers

DEFAULT_BASE_URL = "http://localhost:8085"

# Parameters to set
bundle_name = "rhel"
app_name = "advisor"
event_types = ["new-recommendation", "resolved-recommendation"]
bg_name = "Send Advisor Recommendations to Splunk"

rh_id = {
    'entitlements': {},
    'identity': {
        'account_number': '6089719', 'type': 'User',
        'user': {
            'email': 'jdoe@acme.org', 'first_name': 'Joe',
            'is_active': False, 'is_internal': False,
            'is_org_admin': False, 'last_name': 'Doe',
            'username': 'jdoe'
        }
    },
    'username': 'jdoe'
}

x_rh_id = base64.b64encode(json.dumps(rh_id, separators=(',', ':')).encode())

if (len(sys.argv) < 3):
    print(f'Usage: {sys.argv[0]} SPLUNK_URL SPLUNK_TOKEN [BASE_URL]')
    print()
    print(f'  BASE_URL defaults to {DEFAULT_BASE_URL}')
    print('   append path /api/notifications if going via TurnPike')
    sys.exit(1)

splunk_url = sys.argv[1]
splunk_token = sys.argv[2]
base_url = sys.argv[3] if len(sys.argv) > 3 else DEFAULT_BASE_URL

helpers.set_path_prefix(base_url)

bundle_id = helpers.find_bundle(bundle_name)
print(f"Bundle: {bundle_name} {bundle_id}")
if not bundle_id:
    exit('missing bundle')

app_id = helpers.find_application(bundle_id, app_name)
print(f"Application: {app_name} {app_id}")
if not app_id:
    exit('missing application')

print(f">>> create a behavior group {bg_name}")
bg_id = helpers.create_behavior_group(bg_name, bundle_id, x_rh_id)

for event_type in event_types:
    et_id = helpers.find_event_type(app_id, event_type)
    print(f"Event Type: {event_type} {et_id}")
    if not et_id:
        exit('missing event type')

    print(">>> add event type to behavior group")
    helpers.add_event_type_to_behavior_group(et_id, bg_id, x_rh_id)

print(">>> create splunk endpoint")
props = {
    "url": splunk_url,
    "sub_type": "splunk",
    "secret_token": splunk_token
}
ep_id = helpers.create_endpoint("splunk", x_rh_id, props, "camel")

print(">>> link splunk endpoint to behaviour group")
helpers.link_bg_endpoint(bg_id, ep_id, x_rh_id)
