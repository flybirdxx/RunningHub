import React, { useState } from 'react';
import { RotateCw, Sparkles, Monitor, Smartphone, Activity } from 'lucide-react';

export default function App() {
  const [viewMode, setViewMode] = useState('desktop'); // 'desktop' or 'mobile'
  const [playCount, setPlayCount] = useState(0);

  const triggerReplay = () => setPlayCount(c => c + 1);

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col relative overflow-hidden font-sans text-slate-200">
      {/* 注入自定义关键帧动画 */}
      <style dangerouslySetInnerHTML={{ __html: styles }} />

      {/* 顶部控制栏 */}
      <header className="relative z-50 flex justify-between items-center p-6 border-b border-slate-800/50 bg-slate-950/80 backdrop-blur-md">
        <div className="font-bold text-xl flex items-center gap-2">
          <Activity className="text-emerald-400" />
          <span className="bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-blue-500">
            RunningHub
          </span>
        </div>
        
        {/* 设备切换器 */}
        <div className="flex bg-slate-900 p-1 rounded-lg border border-slate-800">
          <button
            onClick={() => { setViewMode('desktop'); triggerReplay(); }}
            className={`flex items-center gap-2 px-4 py-2 rounded-md transition-all text-sm font-medium ${
              viewMode === 'desktop' ? 'bg-slate-800 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'
            }`}
          >
            <Monitor size={16} /> 桌面端视效
          </button>
          <button
            onClick={() => { setViewMode('mobile'); triggerReplay(); }}
            className={`flex items-center gap-2 px-4 py-2 rounded-md transition-all text-sm font-medium ${
              viewMode === 'mobile' ? 'bg-slate-800 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'
            }`}
          >
            <Smartphone size={16} /> 移动端开屏
          </button>
        </div>

        <button
          onClick={triggerReplay}
          className="flex items-center gap-2 px-4 py-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg transition-colors text-sm"
        >
          <RotateCw size={16} /> 重新播放
        </button>
      </header>

      {/* 主展示区 */}
      <main className="flex-1 flex items-center justify-center relative w-full h-full p-6">
        {/* 全局背景纹理 */}
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wNSkiLz48L3N2Zz4=')] opacity-50 pointer-events-none"></div>

        {viewMode === 'desktop' ? (
          <DesktopView key={`desktop-${playCount}`} />
        ) : (
          <MobileSplashView key={`mobile-${playCount}`} />
        )}
      </main>
    </div>
  );
}

// ==========================================
// 1. 桌面端动画组件 (宽屏、大冲击力)
// ==========================================
function DesktopView() {
  return (
    <div className="relative w-full max-w-5xl h-[60vh] flex items-center justify-center">
      {/* 动态背景光晕 */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[300px] bg-emerald-500/10 blur-[100px] rounded-full pointer-events-none"></div>
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[300px] h-[300px] bg-blue-600/10 blur-[80px] rounded-full pointer-events-none"></div>

      {/* 极速光束 (Speed Beam) */}
      <div className="absolute top-1/2 left-0 w-full h-[2px] bg-gradient-to-r from-transparent via-emerald-400 to-transparent animate-speed-flash"></div>

      {/* 重力冲击波 (Shockwave) */}
      <div className="absolute top-1/2 left-[60%] -translate-x-1/2 -translate-y-1/2 rounded-full border border-blue-400 animate-shockwave"></div>

      {/* 品牌核心 Logo 文字 */}
      <div className="flex items-center text-7xl md:text-9xl font-black tracking-tighter drop-shadow-2xl z-10">
        <span 
          className="text-emerald-400 italic pr-2 animate-run-in-extreme relative" 
          style={{ textShadow: '0 0 40px rgba(52,211,153,0.5)' }}
        >
          Running
        </span>
        <div className="relative animate-hub-slam ml-2">
          <span className="relative z-10 bg-blue-600 text-white px-6 py-2 rounded-3xl shadow-[0_0_60px_rgba(37,99,235,0.8)] block">
            Hub
          </span>
        </div>
      </div>

      <Sparkles className="absolute top-[20%] right-[20%] text-emerald-400 animate-sparkle-1 opacity-0" size={32} />
      <Sparkles className="absolute bottom-[25%] left-[25%] text-blue-400 animate-sparkle-2 opacity-0" size={24} />
    </div>
  );
}

// ==========================================
// 2. 移动端开屏动画组件 (竖屏、聚拢感、加载进度)
// ==========================================
function MobileSplashView() {
  return (
    <div className="relative w-[340px] h-[720px] bg-slate-950 rounded-[3rem] border-8 border-slate-800 shadow-[0_0_100px_rgba(0,0,0,0.8)] overflow-hidden flex flex-col items-center justify-center">
      
      {/* 手机顶部刘海区域模拟 */}
      <div className="absolute top-0 w-full flex justify-center z-50">
        <div className="w-32 h-6 bg-slate-800 rounded-b-2xl"></div>
      </div>

      {/* 垂直数据流背景 (模拟自上而下的信息瀑布) */}
      <div className="absolute inset-0 opacity-30">
        <div className="absolute left-[20%] top-[-50%] w-[1px] h-full bg-gradient-to-b from-transparent via-emerald-500 to-transparent animate-vertical-stream"></div>
        <div className="absolute left-[50%] top-[-50%] w-[2px] h-full bg-gradient-to-b from-transparent via-blue-500 to-transparent animate-vertical-stream-fast"></div>
        <div className="absolute left-[80%] top-[-50%] w-[1px] h-full bg-gradient-to-b from-transparent via-emerald-400 to-transparent animate-vertical-stream-slow"></div>
      </div>

      {/* 核心光晕 */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[200px] h-[200px] bg-blue-600/20 blur-[60px] rounded-full animate-pulse-slow"></div>

      {/* Logo 区域 */}
      <div className="relative z-10 flex flex-col items-center mt-[-40px]">
        {/* Running - 移动端采用微弧线下落 */}
        <span 
          className="text-5xl font-black italic tracking-tighter text-emerald-400 mb-[-10px] z-20 animate-mobile-run-in"
          style={{ textShadow: '0 0 20px rgba(52,211,153,0.6)' }}
        >
          Running
        </span>
        
        {/* Hub - 移动端采用中心膨胀爆出 */}
        <div className="relative animate-mobile-hub-pop ml-16">
          <span className="relative z-10 bg-blue-600 text-white px-4 py-1.5 rounded-2xl text-4xl font-black shadow-[0_0_40px_rgba(37,99,235,0.8)] block">
            Hub
          </span>
          <div className="absolute inset-0 rounded-2xl ring-2 ring-white/50 animate-ping opacity-50"></div>
        </div>
      </div>

      {/* 开屏底部加载区域 */}
      <div className="absolute bottom-20 flex flex-col items-center w-full px-12 animate-fade-in-up delay-1000">
        <div className="text-slate-400 text-xs font-semibold tracking-[0.2em] mb-3 animate-pulse">
          SYSTEM STARTING
        </div>
        {/* 进度条 */}
        <div className="w-full h-1 bg-slate-800 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-emerald-400 to-blue-500 animate-loading-bar"></div>
        </div>
      </div>
    </div>
  );
}

// ==========================================
// 3. 全局 CSS 关键帧
// ==========================================
const styles = `
  /* -------------- 桌面端专属动画 -------------- */
  @keyframes speed-flash {
    0% { transform: scaleX(0) translateX(-100%); opacity: 0; }
    30% { transform: scaleX(1.5) translateX(0); opacity: 1; filter: blur(2px); }
    100% { transform: scaleX(0) translateX(100%); opacity: 0; }
  }

  @keyframes run-in-extreme {
    0% { transform: translateX(-150%) skewX(-40deg); opacity: 0; filter: blur(20px); }
    50% { transform: translateX(5%) skewX(-20deg); opacity: 1; filter: blur(0px); }
    70% { transform: translateX(-2%) skewX(-15deg); }
    100% { transform: translateX(0) skewX(-10deg); opacity: 1; filter: blur(0px); }
  }

  @keyframes hub-slam {
    0%, 45% { transform: scale(3) translateY(-50px); opacity: 0; filter: blur(10px); }
    60% { transform: scale(0.8) translateY(10px); opacity: 1; filter: blur(0px); }
    75% { transform: scale(1.1) translateY(-5px); }
    100% { transform: scale(1) translateY(0); opacity: 1; }
  }

  @keyframes shockwave {
    0%, 55% { transform: translate(-50%, -50%) scale(0.5); opacity: 0; border-width: 30px; }
    65% { transform: translate(-50%, -50%) scale(1.5); opacity: 0.8; border-width: 10px; }
    100% { transform: translate(-50%, -50%) scale(4); opacity: 0; border-width: 0px; }
  }

  /* -------------- 移动端开屏专属动画 -------------- */
  @keyframes vertical-stream {
    0% { transform: translateY(-100%); opacity: 0; }
    50% { opacity: 1; }
    100% { transform: translateY(200%); opacity: 0; }
  }

  @keyframes mobile-run-in {
    0% { transform: translate(-50px, -50px) scale(1.5) skewX(-30deg); opacity: 0; filter: blur(10px); }
    40% { transform: translate(10px, 10px) scale(0.9) skewX(-15deg); opacity: 1; filter: blur(0px); }
    60% { transform: translate(-5px, -5px) scale(1.05) skewX(-12deg); }
    100% { transform: translate(0, 0) scale(1) skewX(-10deg); opacity: 1; }
  }

  @keyframes mobile-hub-pop {
    0%, 30% { transform: scale(0); opacity: 0; }
    50% { transform: scale(1.3); opacity: 1; }
    70% { transform: scale(0.9); }
    100% { transform: scale(1); opacity: 1; }
  }

  @keyframes fade-in-up {
    0%, 60% { transform: translateY(20px); opacity: 0; }
    100% { transform: translateY(0); opacity: 1; }
  }

  @keyframes loading-bar {
    0%, 60% { transform: translateX(-100%); }
    85% { transform: translateX(-20%); }
    100% { transform: translateX(0); }
  }

  @keyframes pulse-slow {
    0%, 100% { opacity: 0.5; transform: translate(-50%, -50%) scale(1); }
    50% { opacity: 1; transform: translate(-50%, -50%) scale(1.2); }
  }

  /* -------------- 通用动画 -------------- */
  @keyframes sparkle {
    0%, 80% { opacity: 0; transform: scale(0) rotate(0deg); }
    90% { opacity: 1; transform: scale(1.2) rotate(90deg); }
    100% { opacity: 0.6; transform: scale(1) rotate(180deg); }
  }

  .animate-speed-flash { animation: speed-flash 1.2s ease-in-out forwards; }
  .animate-run-in-extreme { animation: run-in-extreme 1s cubic-bezier(0.1, 0.9, 0.2, 1) forwards; }
  .animate-hub-slam { animation: hub-slam 1.2s cubic-bezier(0.2, 0.8, 0.2, 1) forwards; }
  .animate-shockwave { animation: shockwave 1.5s cubic-bezier(0.1, 0.8, 0.3, 1) forwards; }
  
  .animate-vertical-stream { animation: vertical-stream 2s linear infinite; }
  .animate-vertical-stream-fast { animation: vertical-stream 1.2s linear infinite; animation-delay: 0.5s;}
  .animate-vertical-stream-slow { animation: vertical-stream 3s linear infinite; animation-delay: 0.2s;}
  
  .animate-mobile-run-in { animation: mobile-run-in 1.2s cubic-bezier(0.2, 0.8, 0.2, 1) forwards; }
  .animate-mobile-hub-pop { animation: mobile-hub-pop 1.4s cubic-bezier(0.3, 1.2, 0.3, 1) forwards; }
  .animate-fade-in-up { animation: fade-in-up 2s ease-out forwards; }
  .animate-loading-bar { animation: loading-bar 2.5s ease-out forwards; }
  .animate-pulse-slow { animation: pulse-slow 3s ease-in-out infinite; }

  .animate-sparkle-1 { animation: sparkle 2s ease-out forwards; }
  .animate-sparkle-2 { animation: sparkle 2.2s ease-out forwards; animation-delay: 0.2s; }
`;