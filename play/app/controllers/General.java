package controllers;

/**
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information.
 * <p/>
 * OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier
 * SIG" team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 * <p/>
 * Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)
 * <p/>
 * This file is part of OrbisGIS.
 * <p/>
 * OrbisGIS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * For more information, please consult: <http://www.orbisgis.org/> or contact
 * directly: info_at_ orbisgis.org
 */

import constant.Message;
import play.data.*;
import views.html.*;
import play.mvc.*;
import org.orbisgis.server.mapcatalog.*;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import csp.ContentSecurityPolicy;

@ContentSecurityPolicy
public class General extends Controller{
    private static MapCatalog MC = MapCatalogC.getMapCatalog();

    /**
     * Renders the home page
     * @return
     */
    public static Result home() {
        return ok(home.render());
    }

    /**
     * Renders the login page
     * @return
     */
    public static Result login() {
        return ok(login.render(""));
    }

    /**
     * Checks if the login form is correct, and logs in the user
     * @return The home page if success, the login page with error if error.
     * @throws Exception
     */
    public static Result authenticate() {
        String error="";
        try {
            DynamicForm form = Form.form().bindFromRequest();
            String email = form.get("email");
            String password = form.get("password");
            if(email != null && password != null){
                String[] attributes = {"email","password"};
                String[] values = {email,MapCatalog.hasher(password)};
                List<User> list = User.page(MC, attributes, values);
                if(!list.isEmpty()){
                    session().clear();
                    session("email", email);
                    session("id_user", list.get(0).getId_user());
                    return ok(home.render());
                }else{error= Message.ERROR_LOGIN;}
            }
        } catch (NoSuchAlgorithmException e) {
            error= Message.ERROR_GENERAL;
        } catch (SQLException e) {
            error= Message.ERROR_GENERAL;
        }
        return badRequest(login.render(error));
    }

    /**
     * Clear the cookie session
     * @return The login page
     */
    public static Result logout(){
        session().clear();
        return redirect(routes.General.login());
    }

    /**
     * Renders the sign in page only if no one is logged in
     * @return
     */
    public static Result signin(){
        if(session().get("email")!=null){
            flash("error", Message.ERROR_ALREADY_LOGGED);
            return forbidden(home.render());
        }
        return ok(signin.render(""));
    }

    /**
     * Saves the user that just signed in
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static Result signedin() {
        String error="";
        try {
            DynamicForm form = Form.form().bindFromRequest();
            String email = form.get("email");
            String location = form.get("location");
            String name = form.get("name");
            String password = form.get("password");
            String[] attribute = {"email"};
            String[] values = {email};
            List<User> user = User.page(MC, attribute, values);
            if(email!=null && password.length()>=6){ //check the form
                if(user.isEmpty()){ //check if user mail is used
                    User usr = new User(name, email, password, location);
                    usr.save(MC);
                    return ok(home.render());
                }else{error= Message.ERROR_EMAIL_USED;}
            }else{error= Message.ERROR_LOGIN;}
        } catch (SQLException e) {
            error= Message.ERROR_GENERAL;
        } catch (NoSuchAlgorithmException e) {
            error= Message.ERROR_GENERAL;
        }
        return (badRequest(signin.render(error)));
    }

    /**
     * Generates the profile page
     * @return
     */
    @Security.Authenticated(Secured.class)
    public static Result profilePage() {
        try {
            String id_user = session("id_user");
            String[] attributes = {"id_user"};
            String[] values = {id_user};
            User use = User.page(MC, attributes, values).get(0);
            return ok(profile.render(use));
        } catch (SQLException e) {
            flash("error", Message.ERROR_GENERAL);
        }
        return home();

    }

    /**
     * Update the profile of a user
     * @return
     */
    @Security.Authenticated(Secured.class)
    public static Result changeProfile() {
        try {
            String id_user = session("id_user");
            String[] attributes = {"id_user"};
            String[] values = {id_user};
            User temp = User.page(MC, attributes, values).get(0);
            DynamicForm form = Form.form().bindFromRequest();
            String name = form.get("name");
            String email = form.get("email");
            String location = form.get("location");
            String profession = form.get("profession");
            String additional = form.get("additional");
            session("email",email);
            User use = new User(id_user,name,email,temp.getPassword(),location,profession,additional);
            use.update(MC);
            return profilePage();
        } catch (SQLException e) {
            flash("error", Message.ERROR_GENERAL);
        }
        return home();
    }

    /**
     * Generates the page not found
     * @return
     */
    public static Result PageNotFound(){
        return notFound(notFound.render());
    }

    /**
     * Deletes the account of the connected user
     */
    public static Result deleteAccount(){
        try {
            String id_user = session("id_user");
            User.delete(MC, Long.valueOf(id_user));
            session().clear();
            return signin();
        } catch (SQLException e) {
            flash("error", Message.ERROR_GENERAL);
        }
        return home();
    }
}