"""Replays a server cart trace (/slashrails-trace, server-only spike) through the vanilla 26.2
client: InterpolationHandler (3 steps) fed by position packets every N ticks, and the old-behaviour
minecart renderer, which draws the cart at OldMinecartBehavior.getPos() -- snapped onto each rail
block's straight line -- with its yaw taken from the rail direction.

    python scripts/spike_client_model.py <slashrails-trace.csv> [--sync 3 1]

Per sync interval it reports, over ticks where the cart rides a smoothed run:
  * rider turn:  heading change of the rider's per-tick motion (deg/tick; server path = the ideal)
  * rider kink:  |second difference| of the rider position (blocks/tick^2): corners in the path
  * path error:  rider distance from the server's path
  * model offset / yaw error: how far the vanilla cart MODEL sits from the rider, and how far its
    yaw is from the direction of travel (the model follows the zigzag rails, the rider the curve)
Ported from the decompiled 26.2 sources: InterpolationHandler.interpolate, ServerEntity.sendChanges,
OldMinecartBehavior.getPos, AbstractMinecartRenderer.oldExtractState/oldRender.
"""
import argparse, csv, math, sys
from collections import defaultdict

EXITS = {  # AbstractMinecart.EXITS, flat shapes
    "north_south": ((0, 0, -1), (0, 0, 1)), "east_west": ((-1, 0, 0), (1, 0, 0)),
    "south_east": ((0, 0, 1), (1, 0, 0)), "south_west": ((0, 0, 1), (-1, 0, 0)),
    "north_west": ((0, 0, -1), (-1, 0, 0)), "north_east": ((0, 0, -1), (1, 0, 0)),
}


def load(path):
    carts, rails = defaultdict(list), {}
    with open(path, newline="") as f:
        for r in csv.DictReader(f):
            if r["kind"] == "cart":
                carts[int(r["id"])].append((int(r["tick"]), float(r["x"]), float(r["y"]), float(r["z"]), int(r["curve"] or 0)))
            else:
                rails[(int(r["x"]), int(r["y"]), int(r["z"]))] = r["shape"]
    return carts, rails


def get_pos(rails, x, y, z):
    """OldMinecartBehavior.getPos (flat shapes; slopes are never smoothed)."""
    xt, yt, zt = math.floor(x), math.floor(y), math.floor(z)
    if (xt, yt - 1, zt) in rails:
        yt -= 1
    shape = rails.get((xt, yt, zt))
    if shape not in EXITS:
        return None
    e0, e1 = EXITS[shape]
    x0, z0 = xt + 0.5 + e0[0] * 0.5, zt + 0.5 + e0[2] * 0.5
    x1, z1 = xt + 0.5 + e1[0] * 0.5, zt + 0.5 + e1[2] * 0.5
    xd, zd = x1 - x0, z1 - z0
    if xd == 0:
        p = z - zt
    elif zd == 0:
        p = x - xt
    else:
        p = ((x - x0) * xd + (z - z0) * zd) * 2.0
    return (x0 + xd * p, yt + 0.0625, z0 + zd * p)


def get_pos_offs(rails, x, y, z, offs):
    xt, yt, zt = math.floor(x), math.floor(y), math.floor(z)
    if (xt, yt - 1, zt) in rails:
        yt -= 1
    shape = rails.get((xt, yt, zt))
    if shape not in EXITS:
        return None
    e0, e1 = EXITS[shape]
    xd, zd = e1[0] - e0[0], e1[2] - e0[2]
    d = math.hypot(xd, zd)
    return get_pos(rails, x + xd / d * offs, yt, z + zd / d * offs)


def client_track(server, sync):
    """Client cart position per tick: packet every `sync` ticks, 3-step interpolation."""
    out, pos, target, steps = [], server[0], None, 0
    for t, p in enumerate(server):
        if t % sync == 0 and p != (target or pos):   # ServerEntity: send only if moved
            target, steps = p, 3
        if steps > 0:
            a = 1.0 / steps
            pos = tuple(c + (g - c) * a for c, g in zip(pos, target))
            steps -= 1
        out.append(pos)
    return out


def heading(a, b):
    return math.atan2(b[2] - a[2], b[0] - a[0])


def ang(d):
    return abs((math.degrees(d) + 180) % 360 - 180)


def seg_dist(p, a, b):
    ax, az, bx, bz = a[0], a[2], b[0], b[2]
    vx, vz = bx - ax, bz - az
    L = vx * vx + vz * vz
    t = 0 if L == 0 else max(0, min(1, ((p[0] - ax) * vx + (p[2] - az) * vz) / L))
    return math.hypot(p[0] - (ax + vx * t), p[2] - (az + vz * t))


def stats(xs):
    xs = sorted(xs)
    if not xs:
        return "n/a"
    return f"max {xs[-1]:.3f}  p95 {xs[int(0.95 * (len(xs) - 1))]:.3f}  mean {sum(xs) / len(xs):.3f}"


def analyse(server, curve, rails, sync, label):
    c = server if sync == 0 else client_track(server, sync)
    turn, kink, err, moff, myaw = [], [], [], [], []
    for t in range(2, len(c)):
        if not curve[t] or not curve[t - 2]:
            continue
        v1 = (c[t - 1][0] - c[t - 2][0], c[t - 1][2] - c[t - 2][2])
        v2 = (c[t][0] - c[t - 1][0], c[t][2] - c[t - 1][2])
        if math.hypot(*v1) < 0.05 or math.hypot(*v2) < 0.05:
            continue
        turn.append(ang(math.atan2(v2[1], v2[0]) - math.atan2(v1[1], v1[0])))
        kink.append(math.hypot(v2[0] - v1[0], v2[1] - v1[1]))
        err.append(min(seg_dist(c[t], server[i], server[i + 1]) for i in range(max(0, t - 8), min(len(server) - 1, t + 2))))
        m = get_pos(rails, *c[t])
        if m is not None:
            moff.append(math.hypot(m[0] - c[t][0], m[2] - c[t][2]))
            f, b = get_pos_offs(rails, *c[t], 0.3), get_pos_offs(rails, *c[t], -0.3)
            if f and b and (f[0], f[2]) != (b[0], b[2]):
                d = ang(heading(f, b) - math.atan2(v2[1], v2[0]))
                myaw.append(min(d, 180 - d))   # the model is symmetric end to end
    print(f"  {label:<22} ticks {len(turn)}")
    print(f"    rider turn  deg/tick  {stats(turn)}")
    print(f"    rider kink  b/t^2     {stats(kink)}")
    print(f"    path error  blocks    {stats(err)}")
    if sync:
        print(f"    model offset blocks   {stats(moff)}")
        print(f"    model yaw err deg     {stats(myaw)}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("trace")
    ap.add_argument("--sync", type=int, nargs="+", default=[3, 1])
    ap.add_argument("--min-curve-ticks", type=int, default=20)
    a = ap.parse_args()
    carts, rails = load(a.trace)
    for cid, rows in sorted(carts.items()):
        rows.sort()
        curve = [r[4] for r in rows]
        if sum(curve) < a.min_curve_ticks:
            continue
        server = [(r[1], r[2], r[3]) for r in rows]
        print(f"cart {cid}: {len(rows)} ticks, {sum(curve)} on a smoothed run")
        analyse(server, curve, rails, 0, "server (ideal)")
        for n in a.sync:
            analyse(server, curve, rails, n, f"vanilla client, sync {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
