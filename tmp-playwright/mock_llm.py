from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json

class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def do_POST(self):
        length = int(self.headers.get('Content-Length', '0'))
        body = self.rfile.read(length)
        payload = json.loads(body or '{}')
        messages = payload.get('messages', [])
        user_prompt = messages[-1].get('content', '') if messages else ''
        content = {
            'choices': [
                {
                    'message': {
                        'content': '这是本地测试 LLM 响应。\n如果上方有工具执行结果，请优先参考工具卡片。\n用户问题：' + user_prompt[:120]
                    }
                }
            ]
        }
        encoded = json.dumps(content, ensure_ascii=False).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(encoded)))
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, format, *args):
        return

ThreadingHTTPServer(('127.0.0.1', 18080), Handler).serve_forever()
