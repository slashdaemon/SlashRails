import socket, struct, sys
def pkt(i, t, body):
    b = body.encode('utf8') + b'\x00\x00'
    return struct.pack('<iii', len(b) + 8, i, t) + b
def recv(s):
    n = struct.unpack('<i', s.recv(4))[0]
    data = b''
    while len(data) < n: data += s.recv(n - len(data))
    i, t = struct.unpack('<ii', data[:8])
    return i, data[8:-2].decode('utf8', 'replace')
port = int(sys.argv[1]); pw = sys.argv[2]; cmd = ' '.join(sys.argv[3:])
s = socket.create_connection(('127.0.0.1', port), timeout=10)
s.sendall(pkt(1, 3, pw)); recv(s)
s.sendall(pkt(2, 2, cmd)); print(recv(s)[1])
