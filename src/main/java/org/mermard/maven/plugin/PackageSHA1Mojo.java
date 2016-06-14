package org.mermard.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "package-sha1", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PackageSHA1Mojo extends AbstractMojo {

	@Parameter(required = true, defaultValue = "${project.build.directory}")
	private File outputDirectory;

	@Parameter(required = true, defaultValue = "")
	private String SHA1FileDirectory;

	@Parameter(required = true, defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}.${project.packaging}")
	private File packageEntity;

	public void execute() throws MojoExecutionException {
		File f = outputDirectory;

		// 创建目录
		if (f.exists()) {
			this.deleteFile(f);
			f.mkdirs();
		} else if (!f.exists()) {
			f.mkdirs();
		}

		unzip();// 解压包

		writeSHA1();// 写SHA1

		zip();// 压缩包

		// 删除目录
		this.deleteFile(f);
	}

	/**
	 * 解压包
	 * 
	 * @throws MojoExecutionException
	 */
	private void unzip() throws MojoExecutionException {

		this.getLog().debug("开始解压" + packageEntity.getPath());
		try {
			ZipFileUtil.unzip(packageEntity.getPath(),
					outputDirectory.getPath() + "/");
		} catch (Exception e1) {
			this.getLog().error("解压出错", e1);
			e1.printStackTrace();
			throw new MojoExecutionException("解压出错 " + packageEntity.getPath(),
					e1);
		}
	}

	/**
	 * 生成SHA1文件
	 * 
	 * @throws MojoExecutionException
	 */
	private void writeSHA1() throws MojoExecutionException {
		this.getLog().debug("计算sha1");
		String sha1 = new SHA1().getDigestOfFile(packageEntity);
		this.getLog().debug("计算出的sha1:" + sha1);

		this.getLog().debug("开始写入sha1文件");
		File sha1FilePath = new File(outputDirectory.getPath() + "/"
				+ SHA1FileDirectory);
		if (!sha1FilePath.exists())
			sha1FilePath.mkdirs();
		File touch = new File(sha1FilePath, "SHA1");

		FileWriter w = null;
		try {
			if (!touch.exists())
				touch.createNewFile();
			w = new FileWriter(touch);

			w.write(sha1);
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Error creating file " + touch, e);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		this.getLog().debug("写入sha1文件完成");
	}

	/**
	 * 压缩原包
	 * 
	 * @throws MojoExecutionException
	 */
	private void zip() throws MojoExecutionException {
		this.getLog().debug("开始压缩");
		try {
			ZipFileUtil.zip(outputDirectory.getPath(), packageEntity.getPath());
		} catch (Exception e) {
			this.getLog().error("压缩出错", e);
			e.printStackTrace();
			throw new MojoExecutionException("压缩出错 "
					+ outputDirectory.getPath(), e);
		}
		this.getLog().debug("压缩完成");
	}

	/**
	 * 递归删除所有文件
	 * 
	 * @param f
	 */
	private void deleteFile(File f) {
		if (f.isDirectory()) {
			for (File file : f.listFiles()) {
				this.deleteFile(file);
			}
		}
		f.delete();
	}

}
