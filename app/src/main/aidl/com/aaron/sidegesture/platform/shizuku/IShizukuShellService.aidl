package com.aaron.sidegesture.platform.shizuku;

import com.aaron.sidegesture.platform.shell.ShellResult;

interface IShizukuShellService {
    ShellResult execute(String command) = 1;
    void destroy() = 16777114;
}
