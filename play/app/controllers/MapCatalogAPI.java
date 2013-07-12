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

import play.mvc.*;
import org.orbisgis.server.mapcatalog.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import csp.ContentSecurityPolicy;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Class for REST APi, this will changes when the API will be rewritten with database
 */
@ContentSecurityPolicy
public class MapCatalogAPI extends Controller {
    private static MapCatalog MC = MapCatalogC.getMapCatalog();

    /**
     * Returns a context
     * @param workspace of the workspace, not used
     * @param id the id of the context
     * @return
     */
    public static Result getContext(String workspace, String id){
        try {
            String[] attributes = {"id_owscontext"};
            String[] values = {id};
            List<OWSContext> list = OWSContext.page(MC, attributes, values);
            if(list.isEmpty()){
                return badRequest();
            }else{
                return ok(list.get(0).getContent(MC));
            }
        } catch (SQLException e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * Deletes a context
     * @param workspace of the workspace, not used
     * @param id the id of the context
     * @return
     */
    public static Result deleteContext(String workspace, String id){
        try {
            OWSContext.delete(MC, Long.valueOf(id));
            return noContent();
        } catch (SQLException e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * Get the list of workspaces
     * @return
     */
    public static Result listWorkspaces(){
        try {
            return ok(MC.getWorkspaceList());
        } catch (SQLException e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * Get the context list of a workspace
     * @param id_workspace the id of the workspace
     * @return
     */
    public static Result listContexts(String id_workspace){
        try {
            return ok(MC.getContextList(id_workspace));
        } catch (SQLException e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * adds a context with root as parent
     * @param id_workspace the root workspace
     * @return
     */
   @BodyParser.Of(BodyParser.Xml.class)
    public static Result addContextFromRoot(String id_workspace){
       try {
           Http.RequestBody body = request().body();
           ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
           Source xmlSource = new DOMSource(body.asXml());
           try{
               StreamResult outputTarget = new StreamResult(outputStream);
               TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
           }catch(Exception e){
               return badRequest(e.getMessage());
           }
           InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
           String title = request().body().asRaw().asFile().getName();
           OWSContext ows = new OWSContext(id_workspace, null, null, title);
           Long id_ows = ows.save(MC, is);
           String answer = "<context id=\""+id_ows+"\" date=\""+DATE+"\">\n" +
                           "  <title>"+id_ows+"</title>\n" +
                           "</context>";
           return created(answer);
       } catch (SQLException e) {
           return badRequest(e.getMessage());
       }
   }

    /**
     * Adds a context with folder as parent
     * @param id_workspace the root workspace
     * @param id_folder the parent folder
     * @return
     */
    @BodyParser.Of(BodyParser.Xml.class)
    public static Result addContextFromParent(String id_workspace, String id_folder){
        try {
            //processing the input
            Http.RequestBody body = request().body();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(body.asXml());
            try{
                StreamResult outputTarget = new StreamResult(outputStream);
                TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            }catch(Exception e){
                return badRequest(e.getMessage());
            }
            InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
            //saving the ows
            String title = request().body().asRaw().asFile().getName();
            OWSContext ows = new OWSContext(id_workspace, id_folder, null, title);
            Long id_ows = ows.save(MC, is);
            //sending response
            String answer = "<context id=\""+id_ows+"\" date=\""+DATE+"\">\n" +
                    "  <title>"+id_ows+"</title>\n" +
                    "</context>";
            return created(answer);
        } catch (SQLException e) {
            return badRequest(e.getMessage());
        }
    }

    /**
     * Updates a context
     * @param id_root Root workspace of the context
     * @param id_owscontext the id of the context to update
     * @return
     */
    @BodyParser.Of(BodyParser.Xml.class)
    public static Result updateContext(String id_root, String id_owscontext){
        try {
            //Processing the input
            Http.RequestBody body = request().body();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(body.asXml());
            try{
                StreamResult outputTarget = new StreamResult(outputStream);
                TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            }catch(Exception e){
                return badRequest(e.getMessage());
            }
            InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
            //looking for the current ows
            String[] attributes = {"id_owscontext"};
            String[] values = {id_owscontext};
            OWSContext current = OWSContext.page(MC, attributes, values).get(0);
            String title = request().body().asRaw().asFile().getName();
            //saving the new one
            OWSContext ows = new OWSContext(id_owscontext, current.getId_root(), current.getId_parent(), current.getId_uploader(), title, current.getDate());
            ows.update(MC, is);
            //sending response
            String answer = "<context id=\""+id_owscontext+"\" date=\""+current.getDate()+"\">\n" +
                            "  <title>"+id_owscontext+"</title>\n" +
                            "</context>";
            return ok(answer);
        } catch (SQLException e) {
            return badRequest(e.getMessage());
        }
    }
}
