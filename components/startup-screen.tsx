"use client";

import React, { useState, useEffect } from "react";
import { LumaSpin } from "@/components/ui/luma-spin";

interface StartupScreenProps {
  appName?: string;
  tagline?: string;
  durationMs?: number;
  onFinish?: () => void;
  children?: React.ReactNode;
}

export const StartupScreen: React.FC<StartupScreenProps> = ({
  appName = "LQD",
  tagline = "Expense Manager",
  durationMs = 1600,
  onFinish,
  children,
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [isSwipingUp, setIsSwipingUp] = useState(false);

  useEffect(() => {
    // Stage 1: Trigger smooth swipe up dismissal
    const swipeTimer = setTimeout(() => {
      setIsSwipingUp(true);
    }, durationMs);

    // Stage 2: Unmount overlay once swipe completes
    const finishTimer = setTimeout(() => {
      setIsLoaded(true);
      onFinish?.();
    }, durationMs + 750);

    return () => {
      clearTimeout(swipeTimer);
      clearTimeout(finishTimer);
    };
  }, [durationMs, onFinish]);

  return (
    <div className="relative min-h-screen w-full overflow-hidden bg-neutral-50 dark:bg-black text-neutral-900 dark:text-white">
      {/* Main App Content underneath with physical depth scale-in */}
      <div
        className={`min-h-screen w-full transition-all duration-750 ease-[cubic-bezier(0.32,0.72,0,1)] ${
          isSwipingUp
            ? "scale-100 opacity-100 blur-0"
            : "scale-[0.94] opacity-75 blur-[2px] pointer-events-none"
        }`}
      >
        {children}
      </div>

      {/* Startup Screen Overlay with Liquid Smooth Swipe-Up Reveal */}
      {!isLoaded && (
        <div
          className={`fixed inset-0 z-50 flex flex-col items-center justify-center bg-neutral-50 dark:bg-black shadow-[0_30px_60px_rgba(0,0,0,0.85)] transition-transform duration-750 ease-[cubic-bezier(0.32,0.72,0,1)] ${
            isSwipingUp ? "-translate-y-full" : "translate-y-0"
          }`}
        >
          <div className="relative z-10 flex flex-col items-center gap-6">
            {/* Luma Spin Loader */}
            <LumaSpin
              size={72}
              colorClassName="shadow-neutral-900 dark:shadow-white"
            />

            {/* Official LQD Geometric Brand Logo */}
            <svg
              className="h-11 w-auto"
              viewBox="0 0 1187 536"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              role="img"
              aria-label="LQD"
            >
              <g className="fill-[#18181A] dark:fill-white">
                <path d="M0 2H86v446h195l86 86H0Z"/>
                <path d="M730 2H921A266 266 0 0 1 1187 268A266 266 0 0 1 921 534H912L826 448H921A180 180 0 0 0 1101 268A180 180 0 0 0 921 88H816Z"/>
              </g>
              <g fill="#8C9198">
                <path d="M775.9 398.9A268 268 0 1 0 641 517L571.6 447.6A182 182 0 1 1 711.5 334.5Z"/>
                <path d="M500 310L560.8 249.2L845.5 534H724Z"/>
              </g>
            </svg>
          </div>
        </div>
      )}
    </div>
  );
};

export default StartupScreen;

