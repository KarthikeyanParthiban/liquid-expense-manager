"use client";

import { LumaSpin } from "@/components/ui/luma-spin";

export default function DemoOne() {
  return (
    <div className="flex min-h-[300px] w-full items-center justify-center bg-background p-8">
      <LumaSpin size={65} />
    </div>
  );
}
