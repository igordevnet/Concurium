package com.concurium.Controller;

import com.concurium.annotations.*;

@Controller("/api/users")
public class UserController {

    @Get("/")
    public void getAllUsers() {
        System.out.println("Executing GET /api/users/");
    }

    @Get("/active")
    public void getActiveUsers() {
        System.out.println("Executing GET /api/users/active");
    }

    @Post("")
    public void createUser() {
        System.out.println("Executing POST /api/users");
    }

    @Put("/update")
    public void updateUser() {
        System.out.println("Executing PUT /api/users/update");
    }

    @Patch("/status")
    public void patchUserStatus() {
        System.out.println("Executing PATCH /api/users/status");
    }

    @Delete("/remove")
    public void deleteUser() {
        System.out.println("Executing DELETE /api/users/remove");
    }

    @Query("/search")
    public void searchUsers() {
        System.out.println("Executing QUERY /api/users/search");
    }

    // Edge case: intentionally terrible path formatting
    @Get("//settings///")
    public void getSettings() {
        System.out.println("Executing GET /api/users/settings/");
    }
}