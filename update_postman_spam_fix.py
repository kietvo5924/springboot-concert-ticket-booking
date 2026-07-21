import json

file_path = 'api_testing/postman_collection.json'

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

customer_folder = next((item for item in data['item'] if item['name'] == 'Customer APIs'), None)
if customer_folder:
    # Find the spam request
    spam_request = next((item for item in customer_folder['item'] if item['name'] == 'Submit Booking Request (Spam Test)'), None)
    
    if spam_request:
        # Add Pre-request script
        spam_request['event'] = [
            {
                "listen": "prerequest",
                "script": {
                    "exec": [
                        "pm.variables.set('randomReqId', pm.variables.replaceIn('{{$guid}}'));"
                    ],
                    "type": "text/javascript"
                }
            }
        ]
        
        # Replace the body to use {{randomReqId}}
        spam_request['request']['body']['raw'] = '{\n  "userId": 1,\n  "concertId": 1,\n  "ticketCategoryId": 1,\n  "voucherId": null,\n  "requestId": "{{randomReqId}}"\n}'

with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4)
print("Postman collection updated with Pre-request script.")
