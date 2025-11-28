FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY ["ComicApiDod/ComicApiDod.csproj", "ComicApiDod/"]
RUN dotnet restore "ComicApiDod/ComicApiDod.csproj"

COPY ["ComicApiDod/", "ComicApiDod/"]
RUN dotnet build "ComicApiDod/ComicApiDod.csproj" -c Release -o /app/build

RUN dotnet tool install --global dotnet-ef --version 8.0.0
ENV PATH="${PATH}:/root/.dotnet/tools"

# Install EF Core tools
RUN #dotnet ef migrations add InitialCreate --project  "/src/ComicApiDod/ComicApiDod.csproj"
RUN dotnet publish "ComicApiDod/ComicApiDod.csproj" -c Release -o /app/publish
RUN mv /app/publish/ComicApiDod /app/publish/app
RUN ls -la /app/publish


# Build runtime image
FROM mcr.microsoft.com/dotnet/aspnet:8.0
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "ComicApiDod.dll"]

#FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
#WORKDIR /src
#
## Copy csproj and restore dependencies
#COPY ["ComicApiDod/ComicApiDod.csproj", "ComicApiDod/"]
#RUN dotnet restore "ComicApiDod/ComicApiDod.csproj"
#
## Install EF Core tools
#RUN dotnet tool install --global dotnet-ef
#ENV PATH="${PATH}:/root/.dotnet/tools"
#
## Copy the rest of the code
#COPY . .
#WORKDIR "/src/ComicApiDod"
#
## Build and publish
#RUN dotnet build "ComicApiDod.csproj" -c Release -o /app/build
#RUN dotnet publish "ComicApiDod.csproj" -c Release -o /app/publish

# Build runtime image
#FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine
#WORKDIR /app
#COPY --from=build /app/publish .
#ENTRYPOINT ["dotnet", "ComicApiDod.dll"]

#FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
#WORKDIR /app
#EXPOSE 8080
#
#FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
#WORKDIR /src
#COPY ["ComicApiDod/ComicApiDod.csproj", "ComicApiDod/"]
#RUN dotnet restore "ComicApiDod/ComicApiDod.csproj"
#COPY . .
#WORKDIR "/src/ComicApiDod"
#RUN dotnet build "ComicApiDod.csproj" -c Release -o /app/build
#
#FROM build AS publish
#RUN dotnet publish "ComicApiDod.csproj" -c Release -o /app/publish /p:UseAppHost=false
#
## Install EF Core tools in the publish stage (which has SDK)
#RUN dotnet tool uninstall --global dotnet-ef || true && \
#    dotnet tool install --global dotnet-ef --version 8.0.0
#
## Use SDK image for final stage so dotnet ef works
#FROM mcr.microsoft.com/dotnet/sdk:8.0 AS final
#WORKDIR /app
#
## Install netcat for database connection checking
#RUN apt-get update && \
#    apt-get install -y --no-install-recommends netcat-openbsd && \
#    rm -rf /var/lib/apt/lists/*
#
## Install EF Core tools
#RUN dotnet tool install --global dotnet-ef --version 8.0.0
#ENV PATH="${PATH}:/root/.dotnet/tools"
#
#COPY --from=publish /app/publish .
#
## Create entrypoint script to run migrations and start the app
#COPY ComicApiDod/docker-entrypoint.sh /app/
#RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && \
#    chmod +x /app/docker-entrypoint.sh
#
#ENTRYPOINT ["/app/docker-entrypoint.sh"]