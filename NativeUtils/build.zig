const std = @import("std");

pub fn build(b: *std.Build) void {
    const targets: []const struct { triple: []const u8, dir: []const u8 } = &.{
        .{ .triple = "x86_64-windows",     .dir = "windowsx64" },
        .{ .triple = "x86-windows",         .dir = "windowsx86" },
        .{ .triple = "aarch64-windows",     .dir = "windowsarm64" },
        .{ .triple = "x86_64-linux-gnu",    .dir = "linuxx64" },
        .{ .triple = "x86-linux-gnu",       .dir = "linuxx86" },
        .{ .triple = "aarch64-linux-gnu",   .dir = "linuxarm64" },
        .{ .triple = "arm-linux-gnueabihf", .dir = "linuxarmv7" },
    };

    const optimize = b.standardOptimizeOption(.{ .preferred_optimize_mode = .ReleaseFast });
    const all_step = b.step("all", "Build for all platforms");

    for (targets) |t| {
        const query = std.Target.Query.parse(.{ .arch_os_abi = t.triple }) catch @panic("bad triple");
        const resolved = b.resolveTargetQuery(query);
        const is_windows = std.mem.indexOf(u8, t.triple, "windows") != null;

        const mod = b.createModule(.{
            .target = resolved,
            .optimize = optimize,
            .link_libc = true,
            .link_libcpp = true,
        });

        const lib = b.addLibrary(.{
            .linkage = .dynamic,
            .name = "NativeUtils",
            .root_module = mod,
        });

        mod.addCSourceFile(.{ .file = b.path("dllmain.cpp") });
        mod.addIncludePath(b.path("."));

        const basename = if (is_windows) "NativeUtils.dll" else "libNativeUtils.so";

        const install = b.addInstallFileWithDir(
            lib.getEmittedBin(),
            .{ .custom = t.dir },
            basename,
        );
        all_step.dependOn(&install.step);
    }
}
