package io.github.findanonymity.fa;

interface IUserService {
    // Returns a JSON-encoded ShellResult (see core.exec.ShellResult), executed in the
    // Shizuku-privileged (shell or root) process.
    String exec(String command) = 1;
    void destroy() = 16777114;
}
