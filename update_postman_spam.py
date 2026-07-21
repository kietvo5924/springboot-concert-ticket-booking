import json

file_path = 'api_testing/postman_collection.json'

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

customer_folder = next((item for item in data['item'] if item['name'] == 'Customer APIs'), None)
if customer_folder:
    # Find the original Submit Booking Request
    submit_request = next((item for item in customer_folder['item'] if item['name'] == 'Submit Booking Request'), None)
    
    if submit_request:
        import copy
        spam_request = copy.deepcopy(submit_request)
        spam_request['name'] = 'Submit Booking Request (Spam Test)'
        
        # Replace the body to use {{$guid}}
        body_str = spam_request['request']['body']['raw']
        body_str = body_str.replace('"req-uuid-12345"', '"{{$guid}}"')
        spam_request['request']['body']['raw'] = body_str
        
        # Only add if it doesn't exist
        if not any(item['name'] == 'Submit Booking Request (Spam Test)' for item in customer_folder['item']):
            customer_folder['item'].insert(1, spam_request)

with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4)
print("Postman collection updated successfully with Spam Test request.")
