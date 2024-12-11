package com.example.transferly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import org.mindrot.jbcrypt.BCrypt;


public class UserDatabaseHelper {
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public UserDatabaseHelper(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // Deschide baza de date
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    // Închide baza de date
    public void close() {
        dbHelper.close();
    }

    // Adaugă un nou utilizator (username + parolă criptată)
    public long addUser(String username, String password) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashPassword(password));
        return database.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    // Verifică dacă un utilizator cu username și parolă există
    public boolean validateUser(String username, String password) {
        Cursor cursor = database.query(DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_USERNAME + "=?",
                new String[]{username},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String storedPasswordHash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD));
            cursor.close();
            return checkPassword(password, storedPasswordHash);
        }
        return false;
    }

    // Criptarea parolei folosind bcrypt
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Verificarea parolei
    private boolean checkPassword(String password, String storedPasswordHash) {
        return BCrypt.checkpw(password, storedPasswordHash);
    }
}
