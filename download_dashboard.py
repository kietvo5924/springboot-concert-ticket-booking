import os
import urllib.request
import json

# Create directories
os.makedirs('grafana/provisioning/datasources', exist_ok=True)
os.makedirs('grafana/provisioning/dashboards', exist_ok=True)

# Write datasource.yml
datasource_yaml = '''apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
'''
with open('grafana/provisioning/datasources/datasource.yml', 'w') as f:
    f.write(datasource_yaml)

# Write dashboard.yml
dashboard_yaml = '''apiVersion: 1
providers:
  - name: 'Spring Boot Dashboards'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /etc/grafana/provisioning/dashboards
'''
with open('grafana/provisioning/dashboards/dashboard.yml', 'w') as f:
    f.write(dashboard_yaml)

# Download JVM Micrometer dashboard (ID: 4701)
url = 'https://grafana.com/api/dashboards/4701/revisions/9/download'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        dashboard_data = json.loads(response.read().decode())
        
        # Overwrite the hardcoded datasource in the downloaded JSON to use our Prometheus
        if '__inputs' in dashboard_data:
            del dashboard_data['__inputs']
        if '__requires' in dashboard_data:
            del dashboard_data['__requires']
        
        # Replace ${DS_PROMETHEUS} with Prometheus
        dashboard_str = json.dumps(dashboard_data).replace('${DS_PROMETHEUS}', 'Prometheus')
        
        with open('grafana/provisioning/dashboards/jvm-micrometer.json', 'w') as f:
            f.write(dashboard_str)
    print('Dashboard downloaded and configured successfully.')
except Exception as e:
    print(f'Error downloading dashboard: {e}')
