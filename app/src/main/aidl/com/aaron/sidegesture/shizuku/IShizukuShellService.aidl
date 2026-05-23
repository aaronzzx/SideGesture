package com.aaron.sidegesture.shizuku;

import com.aaron.sidegesture.shizuku.ShellResult;

interface IShizukuShellService {
    ShellResult execute(String command) = 1;
    void destroy() = 16777114;
}
