"use client";

import { useEffect, useRef } from "react";

type Particle = {
  x: number;
  y: number;
  vx: number;
  vy: number;
  r: number;
  /** 节点亮度脉动相位 */
  phase: number;
};

type Pulse = {
  /** 起点节点索引 */
  from: number;
  /** 终点节点索引 */
  to: number;
  /** 0-1 进度 */
  t: number;
  /** 推进速度（每帧推进） */
  speed: number;
  /** 颜色色调：0=teal，1=cyan，2=violet */
  hue: number;
};

const COLORS = [
  { core: "rgba(94, 234, 212, ", glow: "rgba(45, 212, 191, " }, // teal
  { core: "rgba(125, 211, 252, ", glow: "rgba(56, 189, 248, " }, // cyan
  { core: "rgba(196, 181, 253, ", glow: "rgba(167, 139, 250, " }, // violet
];

type Props = {
  /** 每像素粒子密度，越大越密 */
  density?: number;
  /** 连线最大距离（像素） */
  maxLink?: number;
  /** 同时存在的脉冲上限 */
  maxPulses?: number;
};

export function ParticleField({
  density = 0.00012,
  maxLink = 150,
  maxPulses = 6,
}: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const mouseRef = useRef<{ x: number; y: number; active: boolean }>({
    x: -9999,
    y: -9999,
    active: false,
  });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d", { alpha: true });
    if (!ctx) return;

    // 减少动效偏好：直接渲染一帧静态网格后退出
    const reduceMotion =
      typeof window !== "undefined" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    let particles: Particle[] = [];
    let pulses: Pulse[] = [];
    let raf = 0;
    let running = true;
    const dpr = Math.min(typeof window !== "undefined" ? window.devicePixelRatio || 1 : 1, 2);

    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      canvas.width = Math.max(1, Math.floor(rect.width * dpr));
      canvas.height = Math.max(1, Math.floor(rect.height * dpr));
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

      const target = Math.max(
        50,
        Math.min(180, Math.floor(rect.width * rect.height * density)),
      );
      particles = Array.from({ length: target }, () => ({
        x: Math.random() * rect.width,
        y: Math.random() * rect.height,
        vx: (Math.random() - 0.5) * 0.22,
        vy: (Math.random() - 0.5) * 0.22,
        r: Math.random() * 1.2 + 0.5,
        phase: Math.random() * Math.PI * 2,
      }));
      pulses = [];
    };

    resize();
    window.addEventListener("resize", resize);

    const onMove = (e: PointerEvent) => {
      const rect = canvas.getBoundingClientRect();
      mouseRef.current = {
        x: e.clientX - rect.left,
        y: e.clientY - rect.top,
        active: true,
      };
    };
    const onLeave = () => {
      mouseRef.current.active = false;
    };

    const parent = canvas.parentElement ?? canvas;
    parent.addEventListener("pointermove", onMove);
    parent.addEventListener("pointerleave", onLeave);

    // 离开标签页时暂停渲染，回来恢复
    const onVisibility = () => {
      if (document.hidden) {
        running = false;
        cancelAnimationFrame(raf);
      } else if (!running) {
        running = true;
        raf = requestAnimationFrame(draw);
      }
    };
    document.addEventListener("visibilitychange", onVisibility);

    // 周期性生成"数据流脉冲"：在两个邻近节点之间传播
    const trySpawnPulse = () => {
      if (pulses.length >= maxPulses) return;
      if (Math.random() > 0.04) return;
      const i = Math.floor(Math.random() * particles.length);
      const a = particles[i];
      // 找一个距离适中的目标
      let bestJ = -1;
      let bestD = Infinity;
      for (let k = 0; k < 6; k++) {
        const j = Math.floor(Math.random() * particles.length);
        if (j === i) continue;
        const dx = a.x - particles[j].x;
        const dy = a.y - particles[j].y;
        const d2 = dx * dx + dy * dy;
        if (d2 < maxLink * maxLink * 1.2 && d2 < bestD) {
          bestD = d2;
          bestJ = j;
        }
      }
      if (bestJ < 0) return;
      pulses.push({
        from: i,
        to: bestJ,
        t: 0,
        speed: 0.012 + Math.random() * 0.014,
        hue: Math.floor(Math.random() * COLORS.length),
      });
    };

    const draw = () => {
      if (!running) return;
      const rect = canvas.getBoundingClientRect();
      const w = rect.width;
      const h = rect.height;

      // 拖尾式淡入：用半透明黑覆盖代替 clear，营造长曝光感
      ctx.fillStyle = "rgba(5, 6, 12, 0.18)";
      ctx.fillRect(0, 0, w, h);

      const m = mouseRef.current;

      // 更新粒子
      for (const p of particles) {
        p.phase += 0.02;

        // 鼠标排斥
        if (m.active) {
          const dx = p.x - m.x;
          const dy = p.y - m.y;
          const d2 = dx * dx + dy * dy;
          const radius = 130;
          if (d2 < radius * radius) {
            const d = Math.sqrt(d2) || 1;
            const force = (1 - d / radius) * 0.45;
            p.vx += (dx / d) * force;
            p.vy += (dy / d) * force;
          }
        }

        // 阻尼 + 持续微扰动，避免静止
        p.vx *= 0.985;
        p.vy *= 0.985;
        const speed = Math.hypot(p.vx, p.vy);
        if (speed < 0.06) {
          p.vx += (Math.random() - 0.5) * 0.06;
          p.vy += (Math.random() - 0.5) * 0.06;
        }

        p.x += p.vx;
        p.y += p.vy;

        // 边界环绕
        if (p.x < -10) p.x = w + 10;
        else if (p.x > w + 10) p.x = -10;
        if (p.y < -10) p.y = h + 10;
        else if (p.y > h + 10) p.y = -10;
      }

      // 连线：双层（亮+晕），按距离衰减
      ctx.lineCap = "round";
      for (let i = 0; i < particles.length; i++) {
        const a = particles[i];
        for (let j = i + 1; j < particles.length; j++) {
          const b = particles[j];
          const dx = a.x - b.x;
          const dy = a.y - b.y;
          const d2 = dx * dx + dy * dy;
          const max2 = maxLink * maxLink;
          if (d2 < max2) {
            const t = 1 - d2 / max2;
            const alpha = t * 0.22;
            ctx.strokeStyle = `rgba(94, 234, 212, ${alpha})`;
            ctx.lineWidth = 0.6;
            ctx.beginPath();
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
            ctx.stroke();
          }
        }
      }

      // 脉冲：沿连线传播的"数据包"
      if (!reduceMotion) trySpawnPulse();
      for (let i = pulses.length - 1; i >= 0; i--) {
        const pl = pulses[i];
        pl.t += pl.speed;
        if (pl.t >= 1) {
          pulses.splice(i, 1);
          continue;
        }
        const a = particles[pl.from];
        const b = particles[pl.to];
        if (!a || !b) {
          pulses.splice(i, 1);
          continue;
        }
        const x = a.x + (b.x - a.x) * pl.t;
        const y = a.y + (b.y - a.y) * pl.t;
        const c = COLORS[pl.hue];
        // 拖尾
        const tailX = a.x + (b.x - a.x) * Math.max(0, pl.t - 0.18);
        const tailY = a.y + (b.y - a.y) * Math.max(0, pl.t - 0.18);
        const grad = ctx.createLinearGradient(tailX, tailY, x, y);
        grad.addColorStop(0, `${c.glow}0)`);
        grad.addColorStop(1, `${c.core}0.95)`);
        ctx.strokeStyle = grad;
        ctx.lineWidth = 1.6;
        ctx.beginPath();
        ctx.moveTo(tailX, tailY);
        ctx.lineTo(x, y);
        ctx.stroke();
        // 头部光晕
        const halo = ctx.createRadialGradient(x, y, 0, x, y, 8);
        halo.addColorStop(0, `${c.core}0.9)`);
        halo.addColorStop(1, `${c.glow}0)`);
        ctx.fillStyle = halo;
        ctx.beginPath();
        ctx.arc(x, y, 8, 0, Math.PI * 2);
        ctx.fill();
      }

      // 节点：核心 + 软光晕
      for (const p of particles) {
        const pulse = (Math.sin(p.phase) + 1) * 0.5; // 0..1
        const glowR = p.r * (5 + pulse * 2.5);
        const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, glowR);
        g.addColorStop(0, `rgba(45, 212, 191, ${0.35 + pulse * 0.25})`);
        g.addColorStop(0.5, "rgba(45, 212, 191, 0.08)");
        g.addColorStop(1, "rgba(45, 212, 191, 0)");
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(p.x, p.y, glowR, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = `rgba(220, 252, 245, ${0.85 + pulse * 0.15})`;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fill();
      }

      raf = requestAnimationFrame(draw);
    };

    if (reduceMotion) {
      // 静态一帧：黑底 + 节点 + 连线，不再调度
      ctx.fillStyle = "#05060c";
      ctx.fillRect(0, 0, canvas.getBoundingClientRect().width, canvas.getBoundingClientRect().height);
      // 走一次完整 update/draw 不做后续调度
      const restoreSpeed = 0;
      void restoreSpeed;
      // 简化：直接复用 draw 一次，但改写 raf 为一次性
      const oneShot = () => {
        const rect = canvas.getBoundingClientRect();
        ctx.fillStyle = "rgba(5, 6, 12, 1)";
        ctx.fillRect(0, 0, rect.width, rect.height);
        for (let i = 0; i < particles.length; i++) {
          const a = particles[i];
          for (let j = i + 1; j < particles.length; j++) {
            const b = particles[j];
            const dx = a.x - b.x;
            const dy = a.y - b.y;
            const d2 = dx * dx + dy * dy;
            if (d2 < maxLink * maxLink) {
              ctx.strokeStyle = `rgba(94, 234, 212, ${(1 - d2 / (maxLink * maxLink)) * 0.18})`;
              ctx.lineWidth = 0.5;
              ctx.beginPath();
              ctx.moveTo(a.x, a.y);
              ctx.lineTo(b.x, b.y);
              ctx.stroke();
            }
          }
          ctx.fillStyle = "rgba(94, 234, 212, 0.85)";
          ctx.beginPath();
          ctx.arc(a.x, a.y, a.r, 0, Math.PI * 2);
          ctx.fill();
        }
      };
      oneShot();
    } else {
      raf = requestAnimationFrame(draw);
    }

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", resize);
      parent.removeEventListener("pointermove", onMove);
      parent.removeEventListener("pointerleave", onLeave);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [density, maxLink, maxPulses]);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 h-full w-full"
      aria-hidden
    />
  );
}
