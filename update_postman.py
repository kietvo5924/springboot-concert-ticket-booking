import json
import os

file_path = 'api_testing/postman_collection.json'

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

customer_folder = next((item for item in data['item'] if item['name'] == 'Customer APIs'), None)
if customer_folder:
    # Add Pay Order
    pay_order = {
        'name': 'Pay Order',
        'request': {
            'method': 'POST',
            'header': [],
            'url': {
                'raw': 'http://localhost:8080/api/v1/bookings/req-uuid-12345/pay',
                'protocol': 'http',
                'host': ['localhost'],
                'port': '8080',
                'path': ['api', 'v1', 'bookings', 'req-uuid-12345', 'pay']
            }
        },
        'response': []
    }
    if not any(item['name'] == 'Pay Order' for item in customer_folder['item']):
        customer_folder['item'].append(pay_order)

admin_folder = next((item for item in data['item'] if item['name'] == 'Admin APIs'), None)
if admin_folder:
    # Add Get DLQ Messages
    get_dlq = {
        'name': 'Get DLQ Messages',
        'request': {
            'method': 'GET',
            'header': [],
            'url': {
                'raw': 'http://localhost:8080/api/v1/admin/dlq',
                'protocol': 'http',
                'host': ['localhost'],
                'port': '8080',
                'path': ['api', 'v1', 'admin', 'dlq']
            }
        },
        'response': []
    }
    if not any(item['name'] == 'Get DLQ Messages' for item in admin_folder['item']):
        admin_folder['item'].append(get_dlq)
        
    # Add Retry DLQ Message
    retry_dlq = {
        'name': 'Retry DLQ Message',
        'request': {
            'method': 'POST',
            'header': [],
            'url': {
                'raw': 'http://localhost:8080/api/v1/admin/dlq/1/retry',
                'protocol': 'http',
                'host': ['localhost'],
                'port': '8080',
                'path': ['api', 'v1', 'admin', 'dlq', '1', 'retry']
            }
        },
        'response': []
    }
    if not any(item['name'] == 'Retry DLQ Message' for item in admin_folder['item']):
        admin_folder['item'].append(retry_dlq)

with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4)
print("Postman collection updated successfully.")
