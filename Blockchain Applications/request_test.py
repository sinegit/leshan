import requests, re
resp = requests.get("http://10.203.53.254:8080/api/clients/Shammya/40000/0/6052")
# content = resp.text

# stripped = re.sub('<[^<]+?>', '', content)
# print(stripped)
# print(type(stripped))

content = resp.json()
print(content["content"]["value"])